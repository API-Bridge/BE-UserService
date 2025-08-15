package org.example.Usersvc.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.service.DevRateLimitService;
import org.example.Usersvc.service.RateLimitService;
import org.example.Usersvc.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * RateLimitInterceptor 테스트
 */
@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {
    
    @Mock
    private UserService userService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private RateLimitService rateLimitService;
    
    @Mock
    private DevRateLimitService devRateLimitService;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @InjectMocks
    private RateLimitInterceptor rateLimitInterceptor;
    
    private User testUser;
    private StringWriter responseWriter;
    
    @BeforeEach
    void setUp() throws Exception {
        testUser = User.builder()
                .auth0Id("test-auth0-id")
                .userEmail("test@example.com")
                .build();
        testUser.setUserId("1");
        
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }
    
    @Test
    @DisplayName("OPTIONS 요청은 통과시켜야 한다")
    void shouldPassOptionsRequest() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("OPTIONS");
        
        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());
        
        // Then
        assertThat(result).isTrue();
        verifyNoInteractions(userService);
    }
    
    @Test
    @DisplayName("사용자 ID 헤더가 없으면 통과시켜야 한다")
    void shouldPassRequestWithoutUserIdHeader() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-User-ID")).thenReturn(null);
        
        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());
        
        // Then
        assertThat(result).isTrue();
        verifyNoInteractions(userService);
    }
    
    @Test
    @DisplayName("Rate Limit 내의 요청은 통과시켜야 한다")
    void shouldPassRequestWithinRateLimit() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-User-ID")).thenReturn("1");
        when(userService.findByUserId(1L)).thenReturn(testUser);
        when(devRateLimitService.isAllowedPerMinute(testUser)).thenReturn(true);
        when(devRateLimitService.isAllowedPerHour(testUser)).thenReturn(true);
        when(devRateLimitService.isAllowedPerDay(testUser)).thenReturn(true);
        
        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());
        
        // Then
        assertThat(result).isTrue();
        verify(devRateLimitService).isAllowedPerMinute(testUser);
        verify(devRateLimitService).isAllowedPerHour(testUser);
        verify(devRateLimitService).isAllowedPerDay(testUser);
    }
    
    @Test
    @DisplayName("분당 Rate Limit을 초과한 요청은 거부해야 한다")
    void shouldRejectRequestExceedingMinuteRateLimit() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-User-ID")).thenReturn("1");
        when(userService.findByUserId(1L)).thenReturn(testUser);
        when(devRateLimitService.isAllowedPerMinute(testUser)).thenReturn(false);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"success\":false,\"message\":\"요청 빈도 제한을 초과했습니다.\",\"errorCode\":\"API002\"}");
        
        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());
        
        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(429);
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(devRateLimitService).isAllowedPerMinute(testUser);
        verify(devRateLimitService, never()).isAllowedPerHour(testUser);
        verify(devRateLimitService, never()).isAllowedPerDay(testUser);
    }
    
    @Test
    @DisplayName("시간당 Rate Limit을 초과한 요청은 거부해야 한다")
    void shouldRejectRequestExceedingHourRateLimit() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-User-ID")).thenReturn("1");
        when(userService.findByUserId(1L)).thenReturn(testUser);
        when(devRateLimitService.isAllowedPerMinute(testUser)).thenReturn(true);
        when(devRateLimitService.isAllowedPerHour(testUser)).thenReturn(false);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"success\":false,\"message\":\"요청 빈도 제한을 초과했습니다.\",\"errorCode\":\"API002\"}");
        
        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());
        
        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(429);
        verify(devRateLimitService).isAllowedPerMinute(testUser);
        verify(devRateLimitService).isAllowedPerHour(testUser);
        verify(devRateLimitService, never()).isAllowedPerDay(testUser);
    }
    
    @Test
    @DisplayName("일일 Rate Limit을 초과한 요청은 거부해야 한다")
    void shouldRejectRequestExceedingDayRateLimit() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-User-ID")).thenReturn("1");
        when(userService.findByUserId(1L)).thenReturn(testUser);
        when(devRateLimitService.isAllowedPerMinute(testUser)).thenReturn(true);
        when(devRateLimitService.isAllowedPerHour(testUser)).thenReturn(true);
        when(devRateLimitService.isAllowedPerDay(testUser)).thenReturn(false);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"success\":false,\"message\":\"요청 빈도 제한을 초과했습니다.\",\"errorCode\":\"API002\"}");
        
        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());
        
        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(429);
        verify(devRateLimitService).isAllowedPerMinute(testUser);
        verify(devRateLimitService).isAllowedPerHour(testUser);
        verify(devRateLimitService).isAllowedPerDay(testUser);
    }
    
    @Test
    @DisplayName("예외 발생 시 graceful degradation으로 통과시켜야 한다")
    void shouldPassRequestOnException() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-User-ID")).thenReturn("invalid");
        
        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());
        
        // Then
        assertThat(result).isTrue();
    }
}