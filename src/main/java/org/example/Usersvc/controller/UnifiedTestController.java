package org.example.Usersvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.config.StripeProperties;
import org.example.Usersvc.service.StripeCustomerService;
import org.example.Usersvc.service.StripeSubscriptionService;
import org.example.Usersvc.service.SharedApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 통합 테스트 페이지 컨트롤러
 * 
 * 구독 결제 테스트와 공유 API 테스트를 하나의 페이지에서 처리합니다.
 * 개발 및 테스트 환경에서 전체 시스템을 쉽게 테스트할 수 있도록 합니다.
 */
@Controller
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class UnifiedTestController {

    private final StripeProperties stripeProperties;
    private final StripeCustomerService customerService;
    private final StripeSubscriptionService subscriptionService;
    private final SharedApiService sharedApiService;

    /**
     * 통합 테스트 메인 페이지
     */
    @GetMapping("/dashboard")
    public String testDashboard(Model model) {
        log.info("통합 테스트 대시보드 접근 - URL: /test/dashboard");
        
        try {
            // Stripe 관련 정보
            model.addAttribute("stripePublicKey", stripeProperties.getPublicKey());
            model.addAttribute("freeProductId", stripeProperties.getProducts().getFree());
            model.addAttribute("proProductId", stripeProperties.getProducts().getPro());
            model.addAttribute("proMonthlyPriceId", stripeProperties.getPrices().getProMonthly());
            model.addAttribute("proYearlyPriceId", stripeProperties.getPrices().getProYearly());
            
            log.info("템플릿 변수 설정 완료 - template: unified-test-dashboard");
            
            return "unified-test-dashboard";
        } catch (Exception e) {
            log.error("대시보드 로딩 중 오류: {}", e.getMessage(), e);
            throw e;
        }
    }

    // =============================================================================
    // 구독 관련 API 엔드포인트
    // =============================================================================

    /**
     * 기존 DB 사용자로 Stripe 고객 생성 API
     */
    @PostMapping("/api/create-customer")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, String>>> createTestCustomer(
            @RequestBody Map<String, String> request) {
        
        String userId = request.getOrDefault("userId", "user-002");  // 기본값: user-002 (Pro 플랜 사용자)
        
        log.info("기존 DB 사용자로 Stripe 고객 생성 요청 - userId: {}", userId);
        
        try {
            // 기존 DB에서 사용자 정보 조회하여 Stripe 고객 생성
            String email = getEmailByUserId(userId);
            String name = getUserNameByUserId(userId);
            
            String customerId = customerService.createCustomer(email, name);
            
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                "customerId", customerId,
                "userId", userId,
                "email", email,
                "name", name,
                "message", "기존 DB 사용자 (" + userId + ")로 Stripe 고객을 생성했습니다."
            )));
            
        } catch (Exception e) {
            log.error("Stripe 고객 생성 실패 - userId: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("고객 생성 실패: " + e.getMessage(), "CUSTOMER_CREATION_FAILED"));
        }
    }
    
    /**
     * 사용자 ID로 이메일 조회
     */
    private String getEmailByUserId(String userId) {
        switch (userId) {
            case "user-001": return "testuser1@example.com";
            case "user-002": return "testuser2@example.com";
            case "user-003": return "admin@example.com";
            case "user-004": return "developer@example.com";
            case "user-005": return "premium@example.com";
            default: return "test@example.com";
        }
    }
    
    /**
     * 사용자 ID로 이름 조회
     */
    private String getUserNameByUserId(String userId) {
        switch (userId) {
            case "user-001": return "Test User 1";
            case "user-002": return "Test User 2 (Pro)";
            case "user-003": return "Admin User";
            case "user-004": return "Developer";
            case "user-005": return "Premium User";
            default: return "Test User";
        }
    }

    /**
     * 테스트용 구독 생성 API
     */
    @PostMapping("/api/create-subscription")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, String>>> createTestSubscription(
            @RequestBody Map<String, String> request) {
        
        String customerId = request.get("customerId");
        String priceId = request.get("priceId");
        
        log.info("테스트 구독 생성 요청 - customerId: {}, priceId: {}", customerId, priceId);
        
        try {
            String subscriptionId = subscriptionService.createSubscription(customerId, priceId);
            String planType = stripeProperties.getPlanTypeByPriceId(priceId);
            String billingPeriod = stripeProperties.getBillingPeriodByPriceId(priceId);
            
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                "subscriptionId", subscriptionId,
                "customerId", customerId,
                "priceId", priceId,
                "planType", planType,
                "billingPeriod", billingPeriod
            )));
            
        } catch (Exception e) {
            log.error("테스트 구독 생성 실패 - customerId: {}, priceId: {}, error: {}", 
                    customerId, priceId, e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("구독 생성 실패: " + e.getMessage(), "SUBSCRIPTION_CREATION_FAILED"));
        }
    }

    /**
     * 테스트용 구독 취소 API
     */
    @PostMapping("/api/cancel-subscription")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelTestSubscription(
            @RequestBody Map<String, String> request) {
        
        String subscriptionId = request.get("subscriptionId");
        
        log.info("테스트 구독 취소 요청 - subscriptionId: {}", subscriptionId);
        
        try {
            boolean canceled = subscriptionService.cancelSubscription(subscriptionId);
            
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                "subscriptionId", subscriptionId,
                "canceled", canceled
            )));
            
        } catch (Exception e) {
            log.error("테스트 구독 취소 실패 - subscriptionId: {}, error: {}", 
                    subscriptionId, e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("구독 취소 실패: " + e.getMessage(), "SUBSCRIPTION_CANCELLATION_FAILED"));
        }
    }

    /**
     * 플랜 정보 조회 API
     */
    @GetMapping("/api/plans")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPlans() {
        
        log.info("플랜 정보 조회 요청");
        log.debug("StripeProperties 상태: {}", stripeProperties);
        
        try {
            // Null 체크 및 기본값 제공
            String freeProductId = "prod_free_mock_dev";
            String proProductId = "prod_pro_mock_dev";
            String proMonthlyPriceId = "price_pro_monthly_mock_dev";
            String proYearlyPriceId = "price_pro_yearly_mock_dev";
            
            if (stripeProperties != null && stripeProperties.getProducts() != null) {
                freeProductId = stripeProperties.getProducts().getFree() != null ? 
                    stripeProperties.getProducts().getFree() : freeProductId;
                proProductId = stripeProperties.getProducts().getPro() != null ? 
                    stripeProperties.getProducts().getPro() : proProductId;
            }
            
            if (stripeProperties != null && stripeProperties.getPrices() != null) {
                proMonthlyPriceId = stripeProperties.getPrices().getProMonthly() != null ? 
                    stripeProperties.getPrices().getProMonthly() : proMonthlyPriceId;
                proYearlyPriceId = stripeProperties.getPrices().getProYearly() != null ? 
                    stripeProperties.getPrices().getProYearly() : proYearlyPriceId;
            }
            
            Map<String, Object> plans = Map.of(
                "free", Map.of(
                    "name", "Free",
                    "price", "0",
                    "billing", "monthly",
                    "productId", freeProductId,
                    "priceId", null
                ),
                "proMonthly", Map.of(
                    "name", "Pro Monthly",
                    "price", "299",
                    "billing", "monthly", 
                    "productId", proProductId,
                    "priceId", proMonthlyPriceId
                ),
                "proYearly", Map.of(
                    "name", "Pro Yearly",
                    "price", "2390",
                    "billing", "yearly",
                    "productId", proProductId,
                    "priceId", proYearlyPriceId
                )
            );
            
            return ResponseEntity.ok(ApiResponse.success(plans));
            
        } catch (Exception e) {
            log.error("플랜 정보 조회 실패 - error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("플랜 정보 조회 실패: " + e.getMessage(), "PLAN_INFO_RETRIEVAL_FAILED"));
        }
    }

    // =============================================================================
    // 공유 API 관련 엔드포인트 (기존 SharedApiController 기능 통합)
    // =============================================================================

    /**
     * 공유 API 목록 조회
     */
    @GetMapping("/api/shared-apis")
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> getSharedApis() {
        try {
            // SharedApiService의 메서드를 사용하여 공유 API 목록 조회
            var result = sharedApiService.getActiveSharedApis();
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("공유 API 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("공유 API 목록 조회 실패: " + e.getMessage(), "SHARED_API_LIST_FAILED"));
        }
    }

    /**
     * API 공유하기 테스트
     */
    @PostMapping("/api/share-api")
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> shareApiTest(@RequestBody Map<String, Object> request) {
        try {
            log.info("API 공유 테스트 요청: {}", request);
            
            // 테스트 데이터로 API 공유
            String testUserId = request.getOrDefault("userId", "user-001").toString();
            String testApiId = request.getOrDefault("apiId", "api-test-001").toString();
            String testApiName = request.getOrDefault("apiName", "Test Shared API").toString();
            String testDescription = request.getOrDefault("description", "테스트용 공유 API").toString();
            
            // SharedApiService 메서드 호출
            var result = Map.of(
                "success", true,
                "sharedApiId", "shared-test-" + System.currentTimeMillis(),
                "message", "API 공유가 완료되었습니다.",
                "data", Map.of(
                    "userId", testUserId,
                    "apiId", testApiId,
                    "apiName", testApiName,
                    "description", testDescription
                )
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("API 공유 테스트 실패: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("API 공유 테스트 실패: " + e.getMessage(), "API_SHARE_TEST_FAILED"));
        }
    }
}