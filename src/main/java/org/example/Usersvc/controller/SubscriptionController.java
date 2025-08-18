package org.example.Usersvc.controller;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.domain.SubscriptionPlan;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.service.DevRateLimitService;
import org.example.Usersvc.service.RateLimitService;
import org.example.Usersvc.service.UserService;
import org.example.Usersvc.service.UserSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 구독 및 API 사용량 관리 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Subscription & Usage", description = "구독 관리 및 API 사용량 조회 API")
public class SubscriptionController {
    
    private final UserService userService;
    private final UserSubscriptionService subscriptionService;
    private final Environment environment;
    
    @Autowired(required = false)
    private RateLimitService rateLimitService;
    
    @Autowired(required = false) 
    private DevRateLimitService devRateLimitService;
    
    @Value("${stripe.secret-key:sk_test_dummy_key}")
    private String stripeSecretKey;
    
    @Value("${stripe.products.free:prod_Ss3CAEwmeB1QuU}")
    private String freeProductId;
    
    @Value("${stripe.products.pro:prod_Ss3ChLEOuz3Km9}")
    private String proProductId;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        log.info("Stripe API 초기화 완료");
    }
    
    @Operation(
            summary = "사용자 구독 정보 조회",
            description = "사용자의 현재 활성 구독 정보를 조회합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "구독 정보 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/users/{userId}/subscription")
    // @PreAuthorize("hasRole('USER')") // 권한 검증 일시 비활성화
    public ResponseEntity<ApiResponse<UserSubscription>> getUserSubscription(
            @Parameter(description = "사용자 ID", example = "123e4567-e89b-12d3-a456-426614174000") @PathVariable String userId) {
        
        try {
            Optional<User> userOptional = userService.getUserById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            User user = userOptional.get();
            UserSubscription subscription = subscriptionService.getActiveSubscription(user);
            
            return ResponseEntity.ok(ApiResponse.success(subscription));
            
        } catch (Exception e) {
            log.error("구독 정보 조회 중 오류 발생 - userId: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("구독 정보 조회에 실패했습니다.", "SUBSCRIPTION_ERROR"));
        }
    }
    
    @Operation(
            summary = "구독 플랜 변경",
            description = "사용자의 구독 플랜을 변경합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/users/{userId}/subscription")
    // @PreAuthorize("hasRole('USER')") // 권한 검증 일시 비활성화
    public ResponseEntity<ApiResponse<UserSubscription>> changePlan(
            @Parameter(description = "사용자 ID") @PathVariable String userId,
            @Parameter(description = "새로운 플랜명") @RequestParam String planName) {
        
        try {
            Optional<User> userOptional = userService.getUserById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            User user = userOptional.get();
            UserSubscription newSubscription = subscriptionService.changePlan(user, planName);
            
            return ResponseEntity.ok(ApiResponse.success(newSubscription));
            
        } catch (Exception e) {
            log.error("플랜 변경 중 오류 발생 - userId: {}, planName: {}", userId, planName, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("플랜 변경에 실패했습니다.", "PLAN_CHANGE_ERROR"));
        }
    }
    
    @Operation(
            summary = "API 사용량 조회",
            description = "사용자의 현재 API 사용량을 조회합니다. (분당, 시간당, 일일)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/users/{userId}/usage")
    // @PreAuthorize("hasRole('USER')") // 권한 검증 일시 비활성화
    public ResponseEntity<ApiResponse<Map<String, Object>>> getApiUsage(
            @Parameter(description = "사용자 ID") @PathVariable String userId) {
        
        try {
            Optional<User> userOptional = userService.getUserById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            User user = userOptional.get();
            UserSubscription subscription = subscriptionService.getActiveSubscription(user);
            SubscriptionPlan plan = subscription.getPlan();
            
            Map<String, Object> usageInfo = new HashMap<>();
            
            // 현재 사용량 조회
            long minuteUsage, hourUsage, dayUsage;
            
            if (devRateLimitService != null) {
                // 개발 환경
                minuteUsage = devRateLimitService.getCurrentMinuteUsage(user);
                hourUsage = devRateLimitService.getCurrentHourUsage(user);
                dayUsage = devRateLimitService.getCurrentDayUsage(user);
            } else {
                // 프로덕션 환경
                minuteUsage = rateLimitService.getCurrentMinuteUsage(user);
                hourUsage = rateLimitService.getCurrentHourUsage(user);
                dayUsage = rateLimitService.getCurrentDayUsage(user);
            }
            
            // 플랜 정보
            usageInfo.put("plan", Map.of(
                    "name", plan.getPlanName(),
                    "maxApiCount", plan.getMaxApiCount(),
                    "rateLimitPerMinute", plan.getRateLimitPerMinute(),
                    "rateLimitPerHour", plan.getRateLimitPerHour(),
                    "rateLimitPerDay", plan.getRateLimitPerDay()
            ));
            
            // 현재 사용량
            usageInfo.put("currentUsage", Map.of(
                    "minute", Map.of("used", minuteUsage, "limit", plan.getRateLimitPerMinute()),
                    "hour", Map.of("used", hourUsage, "limit", plan.getRateLimitPerHour()),
                    "day", Map.of("used", dayUsage, "limit", plan.getRateLimitPerDay())
            ));
            
            // 사용률 계산
            usageInfo.put("usageRatio", Map.of(
                    "minute", (double) minuteUsage / plan.getRateLimitPerMinute() * 100,
                    "hour", (double) hourUsage / plan.getRateLimitPerHour() * 100,
                    "day", (double) dayUsage / plan.getRateLimitPerDay() * 100
            ));
            
            return ResponseEntity.ok(ApiResponse.success(usageInfo));
            
        } catch (Exception e) {
            log.error("사용량 조회 중 오류 발생 - userId: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("사용량 조회에 실패했습니다.", "USAGE_ERROR"));
        }
    }
    
    /**
     * Stripe Checkout 세션 생성 엔드포인트
     */
    @Operation(
            summary = "Stripe 결제 세션 생성",
            description = "Stripe Checkout 세션을 생성하여 결제 페이지 URL을 반환합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping("/subscription/checkout")
    public ResponseEntity<ApiResponse<Map<String, String>>> createCheckoutSession(
            @RequestBody CreateCheckoutRequest request) {
        
        try {
            log.info("Stripe Checkout 세션 생성 요청 - priceId: {}", request.getPriceId());
            
            String priceId = request.getPriceId();
            if (priceId == null || priceId.isEmpty()) {
                log.warn("Price ID가 제공되지 않았습니다. 요청 본문에 priceId를 포함해주세요.");
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Price ID가 필요합니다.", "MISSING_PRICE_ID"));
            }
            
            // 개발 환경에서는 Mock 응답 반환
            if (isTestMode()) {
                log.info("테스트 모드: Mock Stripe Checkout 세션 생성");
                
                String mockSessionId = "cs_test_mock_" + System.currentTimeMillis();
                String mockCheckoutUrl = "https://stripe-mock-checkout.example.com/checkout/" + mockSessionId;
                
                Map<String, String> response = new HashMap<>();
                response.put("checkoutUrl", mockCheckoutUrl);
                response.put("sessionId", mockSessionId);
                response.put("priceId", priceId);
                
                log.info("Mock Stripe Checkout 세션 생성 완료 - sessionId: {}, url: {}", 
                        mockSessionId, mockCheckoutUrl);
                
                return ResponseEntity.ok(ApiResponse.success(response));
            }
            
            // 프로덕션 환경에서는 실제 Stripe API 호출
            SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(request.getSuccessUrl() != null ? request.getSuccessUrl() : 
                            "http://localhost:8080/integrated-test.html?payment=success")
                    .setCancelUrl(request.getCancelUrl() != null ? request.getCancelUrl() : 
                            "http://localhost:8080/integrated-test.html?payment=cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPrice(priceId)
                                    .build()
                    )
                    .build();
            
            Session session = Session.create(params);
            
            Map<String, String> response = new HashMap<>();
            response.put("checkoutUrl", session.getUrl());
            response.put("sessionId", session.getId());
            response.put("priceId", priceId);
            
            log.info("Stripe Checkout 세션 생성 완료 - sessionId: {}, url: {}", 
                    session.getId(), session.getUrl());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (StripeException e) {
            log.error("Stripe API 오류 - code: {}, message: {}", e.getCode(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("결제 세션 생성 실패: " + e.getMessage(), "STRIPE_ERROR"));
            
        } catch (Exception e) {
            log.error("Checkout 세션 생성 중 예상치 못한 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("결제 처리 중 오류가 발생했습니다.", "CHECKOUT_ERROR"));
        }
    }
    
    /**
     * Stripe 설정 정보 조회 엔드포인트
     */
    @Operation(
            summary = "Stripe 프로덕트 정보 조회",
            description = "결제에 사용할 Stripe 프로덕트 ID를 조회합니다."
    )
    @GetMapping("/subscription/products")
    public ResponseEntity<ApiResponse<Map<String, String>>> getStripeProducts() {
        try {
            Map<String, String> products = new HashMap<>();
            products.put("FREE", freeProductId);
            products.put("PRO", proProductId);
            
            return ResponseEntity.ok(ApiResponse.success(products));
            
        } catch (Exception e) {
            log.error("Stripe 프로덕트 정보 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("프로덕트 정보 조회에 실패했습니다.", "PRODUCT_INFO_ERROR"));
        }
    }
    
    /**
     * Stripe Checkout 요청 DTO
     */
    public static class CreateCheckoutRequest {
        private String priceId;
        private String successUrl;
        private String cancelUrl;
        
        // Getters and Setters
        public String getPriceId() { return priceId; }
        public void setPriceId(String priceId) { this.priceId = priceId; }
        
        public String getSuccessUrl() { return successUrl; }
        public void setSuccessUrl(String successUrl) { this.successUrl = successUrl; }
        
        public String getCancelUrl() { return cancelUrl; }
        public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
    }
    
    /**
     * 테스트 모드인지 확인하는 메소드
     * 개발 환경이거나 Stripe 키가 기본값인 경우 테스트 모드로 판단
     */
    private boolean isTestMode() {
        // 개발 프로필이거나 기본 테스트 키를 사용하는 경우
        return Arrays.asList(environment.getActiveProfiles()).contains("dev") ||
               stripeSecretKey.equals("sk_test_your_test_key_here") ||
               stripeSecretKey.startsWith("sk_test_your_test_key");
    }
}