package org.example.Usersvc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Usersvc.common.logging.UserActionLogger;
import org.example.Usersvc.common.metrics.CustomMetrics;
import org.example.Usersvc.domain.Plan;
import org.example.Usersvc.domain.planName;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.event.publisher.EventPublisherService;
import org.example.Usersvc.repository.PlanRepository;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.example.Usersvc.service.ApiUsageTrackingService;
import org.example.Usersvc.service.DevRateLimitService;
import org.example.Usersvc.service.ProductionRateLimitService;
import org.example.Usersvc.service.StripeSubscriptionService;
import org.example.Usersvc.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserSubscriptionRepository userSubscriptionRepository;

    @MockBean
    private PlanRepository planRepository;

    @MockBean
    private StripeSubscriptionService stripeSubscriptionService;

    @MockBean
    private DevRateLimitService devRateLimitService;

    @MockBean
    private ProductionRateLimitService productionRateLimitService;

    @MockBean
    private ApiUsageTrackingService apiUsageTrackingService;

    @MockBean
    private UserActionLogger userActionLogger;

    @MockBean
    private CustomMetrics customMetrics;

    @MockBean
    private EventPublisherService eventPublisher;

    private User testUser;
    private Plan testFreePlan;
    private Plan testProPlan;
    private UserSubscription testSubscription;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "user-001";
        
        testUser = User.builder()
                .userId(testUserId)
                .auth0Id("auth0|test123")
                .userEmail("test@example.com")
                .createdAt(LocalDateTime.now())
                .build();

        testFreePlan = Plan.builder()
                .planName(planName.FREE)
                .price(java.math.BigDecimal.ZERO)
                .description("Free plan")
                .features("{}")
                .build();

        testProPlan = Plan.builder()
                .planName(planName.PRO)
                .price(java.math.BigDecimal.valueOf(22.00))
                .description("Pro plan")
                .features("{}")
                .build();

        testSubscription = UserSubscription.builder()
                .subscriptionId(UUID.randomUUID().toString())
                .user(testUser)
                .plan(testProPlan)
                .planPaymentDate(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/subscription/current - 현재 사용자 구독 정보 조회 성공")
    @WithMockUser
    void getCurrentUserSubscription_Success() throws Exception {
        // given
        when(userService.getUserById("user-001")).thenReturn(Optional.of(testUser));
        when(userSubscriptionRepository.findActiveSubscriptionByUser(testUser))
                .thenReturn(Optional.of(testSubscription));

        // when & then
        mockMvc.perform(get("/api/subscription/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planName").value("Pro"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.maxCustomApiCount").exists())
                .andExpect(jsonPath("$.data.rateLimitPerMinute").exists());

        verify(userService).getUserById("user-001");
        verify(userSubscriptionRepository).findActiveSubscriptionByUser(testUser);
    }

    @Test
    @DisplayName("GET /api/subscription/current - 구독 없음 (FREE 플랜 반환)")
    @WithMockUser
    void getCurrentUserSubscription_NoSubscription() throws Exception {
        // given
        when(userService.getUserById("user-001")).thenReturn(Optional.of(testUser));
        when(userSubscriptionRepository.findActiveSubscriptionByUser(testUser))
                .thenReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/subscription/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planName").value("Free"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.maxCustomApiCount").exists());

        verify(userService).getUserById("user-001");
        verify(userSubscriptionRepository).findActiveSubscriptionByUser(testUser);
    }

    @Test
    @DisplayName("GET /api/users/{userId}/usage - API 사용량 조회 성공")
    @WithMockUser
    void getApiUsage_Success() throws Exception {
        // given
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(userSubscriptionRepository.findActiveSubscriptionByUser(testUser))
                .thenReturn(Optional.of(testSubscription));
        when(apiUsageTrackingService.getCurrentMinuteUsage(testUser)).thenReturn(5L);
        when(apiUsageTrackingService.getCurrentHourUsage(testUser)).thenReturn(45L);
        when(apiUsageTrackingService.getCurrentDayUsage(testUser)).thenReturn(320L);

        // when & then
        mockMvc.perform(get("/api/users/{userId}/usage", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.plan.name").value("Pro"))
                .andExpect(jsonPath("$.data.currentUsage.minute.used").value(5))
                .andExpect(jsonPath("$.data.currentUsage.hour.used").value(45))
                .andExpect(jsonPath("$.data.currentUsage.day.used").value(320));

        verify(userService).getUserById(testUserId);
        verify(apiUsageTrackingService).getCurrentMinuteUsage(testUser);
    }

    @Test
    @DisplayName("POST /api/subscription/checkout - Stripe Checkout 세션 생성 성공")
    @WithMockUser
    void createCheckoutSession_Success() throws Exception {
        // given
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "priceId", "price_test_12345",
                "successUrl", "http://localhost:8081/success",
                "cancelUrl", "http://localhost:8081/cancel"
        ));

        // when & then (테스트 모드에서는 Mock URL 반환)
        mockMvc.perform(post("/api/subscription/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.checkoutUrl").exists())
                .andExpect(jsonPath("$.data.sessionId").exists())
                .andExpect(jsonPath("$.data.priceId").value("price_test_12345"));
    }

    @Test
    @DisplayName("POST /api/subscription/checkout - Price ID 없음으로 실패")
    @WithMockUser
    void createCheckoutSession_MissingPriceId() throws Exception {
        // given
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "successUrl", "http://localhost:8081/success"
        ));

        // when & then
        mockMvc.perform(post("/api/subscription/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Price ID가 필요합니다."))
                .andExpect(jsonPath("$.errorCode").value("MISSING_PRICE_ID"));
    }

    @Test
    @DisplayName("POST /api/subscription/subscribe - 구독하기 성공")
    @WithMockUser
    void subscribe_Success() throws Exception {
        // given
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(userSubscriptionRepository.findActiveSubscriptionByUser(testUser))
                .thenReturn(Optional.empty()); // No existing subscription

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "userId", testUserId,
                "planName", "PRO"
        ));

        // when & then (테스트 모드에서는 Mock URL 반환)
        mockMvc.perform(post("/api/subscription/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.checkoutUrl").exists())
                .andExpect(jsonPath("$.data.sessionId").exists())
                .andExpect(jsonPath("$.data.planName").value("PRO"))
                .andExpect(jsonPath("$.data.userId").value(testUserId));

        verify(userService).getUserById(testUserId);
        verify(userSubscriptionRepository).findActiveSubscriptionByUser(testUser);
    }

    @Test
    @DisplayName("POST /api/subscription/subscribe - 이미 Pro 플랜인 경우 실패")
    @WithMockUser
    void subscribe_AlreadyProPlan() throws Exception {
        // given
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(userSubscriptionRepository.findActiveSubscriptionByUser(testUser))
                .thenReturn(Optional.of(testSubscription)); // Already has Pro plan

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "userId", testUserId,
                "planName", "PRO"
        ));

        // when & then
        mockMvc.perform(post("/api/subscription/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 Pro 플랜을 사용 중입니다."))
                .andExpect(jsonPath("$.errorCode").value("ALREADY_PRO_PLAN"));

        verify(userService).getUserById(testUserId);
        verify(userSubscriptionRepository).findActiveSubscriptionByUser(testUser);
    }

    @Test
    @DisplayName("POST /api/users/{userId}/subscription/cancel - 구독 취소 성공")
    @WithMockUser
    void cancelSubscription_Success() throws Exception {
        // given
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(userSubscriptionRepository.findActiveSubscriptionByUser(testUser))
                .thenReturn(Optional.of(testSubscription));
        when(planRepository.findByplanName(planName.FREE)).thenReturn(Optional.of(testFreePlan));

        // when & then
        mockMvc.perform(post("/api/users/{userId}/subscription/cancel", testUserId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.message").exists())
                .andExpect(jsonPath("$.data.previousplanName").value("PRO"))
                .andExpect(jsonPath("$.data.newplanName").value("FREE"));

        verify(userService).getUserById(testUserId);
        verify(userSubscriptionRepository).findActiveSubscriptionByUser(testUser);
        verify(planRepository).findByplanName(planName.FREE);
    }

    @Test
    @DisplayName("GET /api/subscription/products - Stripe 프로덕트 정보 조회")
    @WithMockUser
    void getStripeProducts_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/subscription/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.FREE").exists())
                .andExpect(jsonPath("$.data.PRO").exists());
    }

    @Test
    @DisplayName("GET /api/subscription/usage-stats - 사용량 통계 조회 성공")
    @WithMockUser
    void getUsageStats_Success() throws Exception {
        // given
        when(userService.getUserById(testUserId))
                .thenReturn(Optional.of(testUser));
        when(apiUsageTrackingService.getCurrentMinuteUsage(testUser)).thenReturn(5L);
        when(apiUsageTrackingService.getCurrentHourUsage(testUser)).thenReturn(45L);
        when(apiUsageTrackingService.getCurrentDayUsage(testUser)).thenReturn(320L);
        when(apiUsageTrackingService.getMonthlyUsage(testUser)).thenReturn(8500L);

        // when & then
        mockMvc.perform(get("/api/subscription/usage-stats")
                        .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentMinute").value(5))
                .andExpect(jsonPath("$.data.currentHour").value(45))
                .andExpect(jsonPath("$.data.currentDay").value(320))
                .andExpect(jsonPath("$.data.currentMonth").value(8500))
                .andExpect(jsonPath("$.data.userId").value(testUserId));

        verify(userService).getUserById(testUserId);
        verify(apiUsageTrackingService).getCurrentMinuteUsage(testUser);
        verify(apiUsageTrackingService).getCurrentHourUsage(testUser);
        verify(apiUsageTrackingService).getCurrentDayUsage(testUser);
        verify(apiUsageTrackingService).getMonthlyUsage(testUser);
    }

    @Test
    @DisplayName("GET /api/subscription/limits-status - 구독 제한 상태 조회 성공")
    @WithMockUser
    void getLimitsStatus_Success() throws Exception {
        // given
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(userSubscriptionRepository.findActiveSubscriptionByUser(testUser))
                .thenReturn(Optional.of(testSubscription));

        // when & then
        mockMvc.perform(get("/api/subscription/limits-status")
                        .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasActiveSubscription").value(true))
                .andExpect(jsonPath("$.data.planName").value("PRO"))
                .andExpect(jsonPath("$.data.planName").value("Pro"))
                .andExpect(jsonPath("$.data.features.maxCustomApiCount").exists())
                .andExpect(jsonPath("$.data.features.rateLimitPerMinute").exists());

        verify(userService).getUserById(testUserId);
        verify(userSubscriptionRepository).findActiveSubscriptionByUser(testUser);
    }
}