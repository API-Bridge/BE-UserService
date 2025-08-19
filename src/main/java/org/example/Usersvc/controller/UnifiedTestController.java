package org.example.Usersvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.config.StripeProperties;
import org.example.Usersvc.service.StripeCustomerService;
import org.example.Usersvc.service.StripeSubscriptionService;
import org.example.Usersvc.service.SharedApiService;
import org.example.Usersvc.domain.CustomApi;
import org.example.Usersvc.domain.PlanType;
import org.example.Usersvc.repository.CustomApiRepository;
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
    private final CustomApiRepository customApiRepository;

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

    /**
     * 공유 API 전용 테스트 페이지
     */
    @GetMapping("/shared-api")
    public String sharedApiTestPage() {
        log.info("공유 API 전용 테스트 페이지 접근 - URL: /test/shared-api");
        return "shared-api-test";
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
     * API 공유하기 테스트 (실제 데이터베이스 연동)
     */
    @PostMapping("/api/share-api")
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> shareApiTest(@RequestBody Map<String, Object> request) {
        try {
            log.info("API 공유 테스트 요청: {}", request);
            
            // 요청 데이터 추출
            String testUserId = request.getOrDefault("userId", "user-001").toString();
            String testApiId = request.getOrDefault("apiId", "api-test-001").toString();
            String testApiName = request.getOrDefault("apiName", "Test Shared API").toString();
            String testDescription = request.getOrDefault("description", "테스트용 공유 API").toString();
            String planType = request.getOrDefault("planType", "PRO").toString();
            
            try {
                // 실제 SharedApiService를 사용하여 API 공유
                PlanType planTypeEnum = PlanType.fromString(planType);
                var sharedApi = sharedApiService.shareApi(testUserId, testApiId, planTypeEnum, testApiName, testDescription);
                
                var result = Map.of(
                    "success", true,
                    "sharedApiId", sharedApi.getSharedApiId(),
                    "message", "API가 데이터베이스에 성공적으로 공유되었습니다.",
                    "data", Map.of(
                        "sharedApiId", sharedApi.getSharedApiId(),
                        "originalApiId", sharedApi.getOriginalApiId(),
                        "creatorId", sharedApi.getCreatorId(),
                        "apiName", sharedApi.getApiName(),
                        "description", sharedApi.getDescription(),
                        "isActive", sharedApi.isActive(),
                        "createdAt", sharedApi.getCreatedAt().toString()
                    )
                );
                
                return ResponseEntity.ok(ApiResponse.success(result));
                
            } catch (IllegalArgumentException e) {
                // 비즈니스 로직 오류 (이미 공유된 API 등)
                log.warn("API 공유 비즈니스 로직 오류: {}", e.getMessage());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("API 공유 실패: " + e.getMessage(), "BUSINESS_LOGIC_ERROR"));
            }
            
        } catch (Exception e) {
            log.error("API 공유 테스트 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("API 공유 테스트 실패: " + e.getMessage(), "API_SHARE_TEST_FAILED"));
        }
    }

    /**
     * API 공유 취소 테스트 (실제 데이터베이스 연동)
     */
    @DeleteMapping("/api/unshare-api")
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> unshareApiTest(
            @RequestParam String userId,
            @RequestParam String customApiId) {
        try {
            log.info("API 공유 취소 테스트 요청 - userId: {}, customApiId: {}", userId, customApiId);
            
            // 실제 SharedApiService를 사용하여 API 공유 취소
            sharedApiService.unshareApi(userId, customApiId);
            
            var result = Map.of(
                "success", true,
                "message", "API 공유가 데이터베이스에서 성공적으로 취소되었습니다.",
                "data", Map.of(
                    "userId", userId,
                    "customApiId", customApiId,
                    "action", "unshared"
                )
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (IllegalArgumentException e) {
            log.warn("API 공유 취소 비즈니스 로직 오류: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("API 공유 취소 실패: " + e.getMessage(), "BUSINESS_LOGIC_ERROR"));
        } catch (Exception e) {
            log.error("API 공유 취소 테스트 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("API 공유 취소 테스트 실패: " + e.getMessage(), "API_UNSHARE_TEST_FAILED"));
        }
    }

    /**
     * 데이터베이스 공유 API 상태 확인 (디버깅용)
     */
    @GetMapping("/api/debug-shared-apis")
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> debugSharedApis() {
        try {
            // 모든 공유 API 조회 (활성/비활성 포함)
            var allSharedApis = sharedApiService.getActiveSharedApis();
            
            var result = Map.of(
                "success", true,
                "message", "데이터베이스 공유 API 상태 조회 완료",
                "data", Map.of(
                    "totalCount", allSharedApis.size(),
                    "apis", allSharedApis
                )
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("공유 API 디버깅 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("공유 API 디버깅 실패: " + e.getMessage(), "DEBUG_FAILED"));
        }
    }

    /**
     * 완전한 API 공유 테스트 (CustomApi 생성 + 공유)
     */
    @PostMapping("/api/complete-share-test")
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> completeShareTest(@RequestBody Map<String, Object> request) {
        try {
            log.info("완전한 API 공유 테스트 요청: {}", request);
            
            // 요청 데이터 추출
            String testUserId = request.getOrDefault("userId", "user-001").toString();
            String testApiId = request.getOrDefault("apiId", "api-test-001").toString();
            String testApiName = request.getOrDefault("apiName", "Test Shared API").toString();
            String testDescription = request.getOrDefault("description", "테스트용 공유 API").toString();
            String planType = request.getOrDefault("planType", "PRO").toString();
            
            // 1. CustomApi가 존재하는지 확인하고, 없으면 생성
            var existingCustomApi = customApiRepository.findByCustomApiId(testApiId);
            if (existingCustomApi.isEmpty()) {
                log.info("CustomApi를 찾을 수 없어 새로 생성합니다 - apiId: {}", testApiId);
                
                CustomApi newCustomApi = CustomApi.builder()
                        .customApiId(testApiId)
                        .userId(testUserId)
                        .name(testApiName)
                        .description(testDescription)
                        .build();
                
                customApiRepository.save(newCustomApi);
                log.info("CustomApi 생성 완료 - apiId: {}", testApiId);
            } else {
                log.info("기존 CustomApi 발견 - apiId: {}", testApiId);
            }
            
            // 2. API 공유
            try {
                PlanType planTypeEnum = PlanType.fromString(planType);
                var sharedApi = sharedApiService.shareApi(testUserId, testApiId, planTypeEnum, testApiName, testDescription);
                
                var result = Map.of(
                    "success", true,
                    "message", "CustomApi 생성 및 공유가 데이터베이스에 성공적으로 완료되었습니다.",
                    "data", Map.of(
                        "customApiCreated", existingCustomApi.isEmpty(),
                        "sharedApi", Map.of(
                            "sharedApiId", sharedApi.getSharedApiId(),
                            "originalApiId", sharedApi.getOriginalApiId(),
                            "creatorId", sharedApi.getCreatorId(),
                            "apiName", sharedApi.getApiName(),
                            "description", sharedApi.getDescription(),
                            "isActive", sharedApi.isActive(),
                            "createdAt", sharedApi.getCreatedAt().toString()
                        )
                    )
                );
                
                return ResponseEntity.ok(ApiResponse.success(result));
                
            } catch (IllegalArgumentException e) {
                log.warn("API 공유 비즈니스 로직 오류: {}", e.getMessage());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("API 공유 실패: " + e.getMessage(), "BUSINESS_LOGIC_ERROR"));
            }
            
        } catch (Exception e) {
            log.error("완전한 API 공유 테스트 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("완전한 API 공유 테스트 실패: " + e.getMessage(), "COMPLETE_SHARE_TEST_FAILED"));
        }
    }
}