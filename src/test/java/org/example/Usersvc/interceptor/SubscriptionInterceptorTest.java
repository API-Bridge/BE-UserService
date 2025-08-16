package org.example.Usersvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.Usersvc.domain.SubscriptionPlan;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.service.SubscriptionPlanManagementService;
import org.example.Usersvc.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("구독 상태별 기능 제어 인터셉터 테스트")
class SubscriptionInterceptorTest {

    @Mock
    private UserService userService;

    @Mock
    private SubscriptionPlanManagementService planManagementService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HandlerMethod handlerMethod;

    @InjectMocks
    private SubscriptionInterceptor subscriptionInterceptor;


    @Test
    @DisplayName("FREE 플랜 사용자의 API 사용량이 제한 내일 때 요청을 허용해야 한다")
    void preHandle_FreePlan_WithinLimit() throws Exception {
        String userId = "user-12345";
        
        User user = User.builder()
                .userId(userId)
                .auth0Id("auth0|12345")
                .userEmail("test@example.com")
                .build();

        SubscriptionPlan freePlan = SubscriptionPlan.builder()
                .planName("FREE")
                .maxApiCount(5)
                .rateLimitPerMinute(10)
                .rateLimitPerHour(100)
                .rateLimitPerDay(1000)
                .monthlyPrice(0)
                .build();

        UserSubscription freeSubscription = UserSubscription.builder()
                .user(user)
                .plan(freePlan)
                .startedAt(LocalDateTime.now())
                .expiresAt(null)
                .build();

        when(request.getHeader("User-ID")).thenReturn(userId);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(planManagementService.getCurrentSubscription(user)).thenReturn(freeSubscription);
        when(planManagementService.checkRateLimit(user)).thenReturn(true);
        when(planManagementService.checkApiUsageLimit(user, "API_CALL")).thenReturn(true);

        boolean result = subscriptionInterceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isTrue();
        verify(planManagementService).recordApiUsage(user, "API_CALL");
    }

    @Test
    @DisplayName("FREE 플랜 사용자의 API 사용량이 제한을 초과할 때 요청을 거부해야 한다")
    void preHandle_FreePlan_ExceedsLimit() throws Exception {
        // Response writer 설정 (에러 응답을 위해 필요)
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        String userId = "user-12345";
        
        User user = User.builder()
                .userId(userId)
                .auth0Id("auth0|12345")
                .userEmail("test@example.com")
                .build();

        SubscriptionPlan freePlan = SubscriptionPlan.builder()
                .planName("FREE")
                .maxApiCount(5)
                .rateLimitPerMinute(10)
                .rateLimitPerHour(100)
                .rateLimitPerDay(1000)
                .monthlyPrice(0)
                .build();

        UserSubscription freeSubscription = UserSubscription.builder()
                .user(user)
                .plan(freePlan)
                .startedAt(LocalDateTime.now())
                .expiresAt(null)
                .build();

        when(request.getHeader("User-ID")).thenReturn(userId);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(planManagementService.getCurrentSubscription(user)).thenReturn(freeSubscription);
        when(planManagementService.checkRateLimit(user)).thenReturn(true);
        when(planManagementService.checkApiUsageLimit(user, "API_CALL")).thenReturn(false);

        boolean result = subscriptionInterceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(planManagementService, never()).recordApiUsage(any(), any());
    }

    @Test
    @DisplayName("만료된 구독을 가진 사용자의 요청을 거부해야 한다")
    void preHandle_ExpiredSubscription() throws Exception {
        // Response writer 설정 (에러 응답을 위해 필요)
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        String userId = "user-12345";
        
        User user = User.builder()
                .userId(userId)
                .auth0Id("auth0|12345")
                .userEmail("test@example.com")
                .build();

        SubscriptionPlan proPlan = SubscriptionPlan.builder()
                .planName("PRO")
                .maxApiCount(50)
                .rateLimitPerMinute(100)
                .rateLimitPerHour(1000)
                .rateLimitPerDay(10000)
                .monthlyPrice(2999)
                .build();

        UserSubscription expiredSubscription = UserSubscription.builder()
                .user(user)
                .plan(proPlan)
                .startedAt(LocalDateTime.now().minusMonths(2))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(request.getHeader("User-ID")).thenReturn(userId);
        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(planManagementService.getCurrentSubscription(user)).thenReturn(expiredSubscription);

        boolean result = subscriptionInterceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
        verify(planManagementService, never()).recordApiUsage(any(), any());
    }

    @Test
    @DisplayName("사용자 ID가 없는 요청을 거부해야 한다")
    void preHandle_NoUserId() throws Exception {
        // Response writer 설정 (에러 응답을 위해 필요)
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        when(request.getHeader("User-ID")).thenReturn(null);

        boolean result = subscriptionInterceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(userService, never()).getUserById(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 요청을 거부해야 한다")
    void preHandle_UserNotFound() throws Exception {
        // Response writer 설정 (에러 응답을 위해 필요)
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        String userId = "user-12345";

        when(request.getHeader("User-ID")).thenReturn(userId);
        when(userService.getUserById(userId)).thenReturn(Optional.empty());

        boolean result = subscriptionInterceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(planManagementService, never()).getCurrentSubscription(any());
    }

    @Test
    @DisplayName("Rate Limit에 걸린 사용자의 요청을 거부해야 한다")
    void preHandle_RateLimitExceeded() throws Exception {
        // Response writer 설정 (에러 응답을 위해 필요)
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        String userId = "user-12345";
        
        User user = User.builder()
                .userId(userId)
                .auth0Id("auth0|12345")
                .userEmail("test@example.com")
                .build();

        SubscriptionPlan freePlan = SubscriptionPlan.builder()
                .planName("FREE")
                .maxApiCount(5)
                .rateLimitPerMinute(10)
                .rateLimitPerHour(100)
                .rateLimitPerDay(1000)
                .monthlyPrice(0)
                .build();

        UserSubscription freeSubscription = UserSubscription.builder()
                .user(user)
                .plan(freePlan)
                .startedAt(LocalDateTime.now())
                .expiresAt(null)
                .build();

        when(request.getHeader("User-ID")).thenReturn(userId);
        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(planManagementService.getCurrentSubscription(user)).thenReturn(freeSubscription);
        when(planManagementService.checkRateLimit(user)).thenReturn(false);

        boolean result = subscriptionInterceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isFalse();
        verify(response).setStatus(429); // Too Many Requests
        verify(planManagementService, never()).recordApiUsage(any(), any());
    }

    @Test
    @DisplayName("정적 리소스 요청은 통과시켜야 한다")
    void preHandle_StaticResource() throws Exception {
        Object handler = new Object(); // HandlerMethod가 아닌 경우
        
        // 이 테스트는 HttpServletResponse.getWriter()를 호출하지 않으므로 별도 설정 불필요
        HttpServletRequest simpleRequest = mock(HttpServletRequest.class);
        HttpServletResponse simpleResponse = mock(HttpServletResponse.class);

        boolean result = subscriptionInterceptor.preHandle(simpleRequest, simpleResponse, handler);

        assertThat(result).isTrue();
        verify(userService, never()).getUserById(any());
    }
}