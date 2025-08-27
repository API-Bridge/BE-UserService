package org.example.Usersvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.domain.PlanName;
import org.example.Usersvc.service.TossPayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TossPay 결제 컨트롤러
 * 
 * TossPay 결제, 빌링키 등록, 정기결제 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/tosspay")
@RequiredArgsConstructor
public class TossPayController {

    private final TossPayService tossPayService;

    /**
     * 구독 결제 요청 (통합 엔드포인트)
     */
    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> subscribe(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        String planNameStr = (String) request.get("planName");
        String provider = (String) request.getOrDefault("provider", "TOSSPAY");
        
        // 헤더 정보 디버깅 로그
        log.info("🔍 TossPay 구독 요청 수신:");
        log.info("  - X-User-Id 헤더: {}", userId);
        log.info("  - planName: {}", planNameStr);
        log.info("  - provider: {}", provider);
        log.info("  - 모든 헤더: {}", 
            Collections.list(httpRequest.getHeaderNames()).stream()
                .collect(Collectors.toMap(
                    headerName -> headerName,
                    headerName -> httpRequest.getHeader(headerName)
                )));
        
        log.info("결제 요청 - userId: {}, planName: {}, provider: {}", userId, planNameStr, provider);
        
        // 입력값 검증
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("사용자 ID가 필요합니다.", "MISSING_USER_ID"));
        }
        
        if (planNameStr == null || planNameStr.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("플랜명이 필요합니다.", "MISSING_PLAN_NAME"));
        }
        
        try {
            PlanName planName;
            try {
                planName = PlanName.valueOf(planNameStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("유효하지 않은 플랜명입니다: " + planNameStr + ". 사용 가능한 플랜: FREE, PRO", "INVALID_PLAN_NAME"));
            }
            
            Map<String, Object> paymentData = tossPayService.createSubscriptionPayment(userId, planName);
            
            log.info("TossPay 결제 요청 생성 완료 - userId: {}, orderId: {}", 
                userId, paymentData.get("orderId"));
            
            return ResponseEntity.ok(ApiResponse.success(paymentData, "결제 요청이 생성되었습니다."));
            
        } catch (Exception e) {
            log.error("TossPay 구독 결제 요청 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("구독 결제 요청에 실패했습니다.", "TOSSPAY_SUBSCRIPTION_ERROR"));
        }
    }

    /**
     * 결제 성공 처리
     */
    @GetMapping("/success")
    public ResponseEntity<ApiResponse<String>> paymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Integer amount) {
        
        log.info("TossPay 결제 성공 - paymentKey: {}, orderId: {}, amount: {}", 
            paymentKey, orderId, amount);
        
        try {
            tossPayService.confirmPayment(paymentKey, orderId, amount);
            return ResponseEntity.ok(ApiResponse.success("결제가 성공적으로 완료되었습니다.", "PAYMENT_SUCCESS"));
            
        } catch (Exception e) {
            log.error("TossPay 결제 확인 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("결제 확인에 실패했습니다.", "PAYMENT_CONFIRM_ERROR"));
        }
    }

    /**
     * 결제 실패 처리
     */
    @GetMapping("/fail")
    public ResponseEntity<ApiResponse<String>> paymentFail(
            @RequestParam String code,
            @RequestParam String message,
            @RequestParam String orderId) {
        
        log.warn("TossPay 결제 실패 - code: {}, message: {}, orderId: {}", code, message, orderId);
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("결제에 실패했습니다: " + message, code));
    }


    /**
     * 결제 승인 (V2 위젯용)
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmPayment(
            @RequestBody Map<String, Object> request) {
        
        String paymentKey = (String) request.get("paymentKey");
        String orderId = (String) request.get("orderId");
        Integer amount = (Integer) request.get("amount");
        
        log.info("TossPay 결제 승인 요청 - paymentKey: {}, orderId: {}, amount: {}", 
            paymentKey, orderId, amount);
        
        try {
            // TossPay 승인 API 호출 및 DB 업데이트
            Map<String, Object> result = tossPayService.confirmPaymentWithAPI(paymentKey, orderId, amount);
            
            log.info("✅ TossPay 결제 승인 완료 - orderId: {}", orderId);
            
            return ResponseEntity.ok(ApiResponse.success(result, "결제가 성공적으로 승인되었습니다."));
            
        } catch (Exception e) {
            log.error("TossPay 결제 승인 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("결제 승인에 실패했습니다.", "PAYMENT_CONFIRM_FAILED"));
        }
    }

    /**
     * 헤더 테스트용 디버그 엔드포인트
     */
    @GetMapping("/debug/headers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugHeaders(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest httpRequest) {
        
        log.info("🔧 헤더 디버그 요청:");
        log.info("  - X-User-Id 헤더: {}", userId);
        
        Map<String, Object> headerInfo = new HashMap<>();
        headerInfo.put("X-User-Id", userId);
        headerInfo.put("allHeaders", 
            Collections.list(httpRequest.getHeaderNames()).stream()
                .collect(Collectors.toMap(
                    headerName -> headerName,
                    headerName -> httpRequest.getHeader(headerName)
                )));
        
        return ResponseEntity.ok(ApiResponse.success(headerInfo, "헤더 정보를 성공적으로 가져왔습니다."));
    }

    /**
     * TossPay 설정 디버그 엔드포인트
     */
    @GetMapping("/debug/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugConfig() {
        
        log.info("🔧 TossPay 설정 디버그 요청:");
        
        try {
            Map<String, Object> configInfo = new HashMap<>();
            // TossPayService에서 TossPayProperties 접근
            String clientKey = tossPayService.getClientKeyForDebug();
            String secretKey = tossPayService.getSecretKeyForDebug();
            String successUrl = tossPayService.getSuccessUrlForDebug();
            String failUrl = tossPayService.getFailUrlForDebug();
            
            configInfo.put("clientKey", clientKey != null ? clientKey : "NULL");
            configInfo.put("secretKeyLength", secretKey != null ? secretKey.length() : 0);
            configInfo.put("successUrl", successUrl != null ? successUrl : "NULL");
            configInfo.put("failUrl", failUrl != null ? failUrl : "NULL");
            
            log.info("🔧 TossPay 설정:");
            log.info("  - clientKey: {}", clientKey);
            log.info("  - secretKey length: {}", secretKey != null ? secretKey.length() : 0);
            log.info("  - successUrl: {}", successUrl);
            log.info("  - failUrl: {}", failUrl);
            
            return ResponseEntity.ok(ApiResponse.success(configInfo, "TossPay 설정 정보를 성공적으로 가져왔습니다."));
        } catch (Exception e) {
            log.error("TossPay 설정 디버그 오류: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TossPay 설정 디버그 오류: " + e.getMessage(), "DEBUG_CONFIG_ERROR"));
        }
    }

    /**
     * 빌링키 등록 (정기결제용)
     */
    @PostMapping("/billing/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerBilling(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, Object> request) {
        
        String customerKey = (String) request.get("customerKey");
        
        log.info("TossPay 빌링키 등록 요청 - userId: {}, customerKey: {}", userId, customerKey);
        
        try {
            Map<String, Object> billingData = tossPayService.registerBillingKey(userId, customerKey);
            
            return ResponseEntity.ok(ApiResponse.success(billingData, "빌링키 등록 요청이 생성되었습니다."));
            
        } catch (Exception e) {
            log.error("TossPay 빌링키 등록 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("빌링키 등록에 실패했습니다.", "BILLING_REGISTER_ERROR"));
        }
    }

    /**
     * 탈렌더 방식 테스트 - 서버에서 직접 결제 처리
     */
    @PostMapping("/test/direct-payment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testDirectPayment(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, Object> request) {
        
        String planNameStr = (String) request.get("planName");
        String paymentKey = "test_payment_key_" + System.currentTimeMillis();
        
        log.info("탈렌더 테스트 결제 시작 - userId: {}, planName: {}", userId, planNameStr);
        
        try {
            PlanName planName = PlanName.valueOf(planNameStr.toUpperCase());
            
            // 1. 결제 요청 데이터 생성
            Map<String, Object> paymentData = tossPayService.createSubscriptionPayment(userId, planName);
            String orderId = (String) paymentData.get("orderId");
            Integer amount = (Integer) paymentData.get("amount");
            
            log.info("결제 데이터 생성 완료 - orderId: {}, amount: {}", orderId, amount);
            
            // 2. 서버에서 직접 결제 승인 처리 (테스트용 모의)
            Map<String, Object> confirmResult = tossPayService.confirmPaymentWithAPI(paymentKey, orderId, amount);
            
            log.info("✅ 탈렌더 테스트 결제 완료 - orderId: {}", orderId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("paymentKey", paymentKey);
            result.put("orderId", orderId);
            result.put("amount", amount);
            result.put("planName", planNameStr);
            result.put("confirmResult", confirmResult);
            
            return ResponseEntity.ok(ApiResponse.success(result, "탈렌더 테스트 결제가 성공적으로 완료되었습니다."));
            
        } catch (Exception e) {
            log.error("탈렌더 테스트 결제 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("탈렌더 테스트 결제에 실패했습니다: " + e.getMessage(), "DIRECT_PAYMENT_TEST_ERROR"));
        }
    }

    /**
     * TossPay 설정 확인 테스트
     */
    @GetMapping("/test/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testConfig() {
        
        log.info("TossPay 설정 확인 테스트");
        
        try {
            Map<String, Object> configInfo = new HashMap<>();
            configInfo.put("clientKey", tossPayService.getClientKeyForDebug());
            configInfo.put("secretKeyMasked", tossPayService.getSecretKeyForDebug() != null ? 
                tossPayService.getSecretKeyForDebug().substring(0, 10) + "***" : "NULL");
            configInfo.put("successUrl", tossPayService.getSuccessUrlForDebug());
            configInfo.put("failUrl", tossPayService.getFailUrlForDebug());
            
            return ResponseEntity.ok(ApiResponse.success(configInfo, "TossPay 설정 확인 완료"));
            
        } catch (Exception e) {
            log.error("TossPay 설정 확인 오류: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TossPay 설정 확인 오류: " + e.getMessage(), "CONFIG_TEST_ERROR"));
        }
    }

    /**
     * 빌링키로 정기결제 실행
     */
    @PostMapping("/billing/charge")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chargeBilling(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, Object> request) {
        
        String billingKey = (String) request.get("billingKey");
        Integer amount = (Integer) request.get("amount");
        String orderName = (String) request.getOrDefault("orderName", "정기결제");
        
        log.info("TossPay 빌링키 정기결제 요청 - userId: {}, amount: {}, orderName: {}", 
            userId, amount, orderName);
        
        try {
            Map<String, Object> chargeResult = tossPayService.chargeWithBillingKey(userId, billingKey, amount, orderName);
            
            if ("SUCCESS".equals(chargeResult.get("status"))) {
                return ResponseEntity.ok(ApiResponse.success(chargeResult, "정기결제가 성공적으로 완료되었습니다."));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error((String) chargeResult.get("message"), "BILLING_CHARGE_FAILED"));
            }
            
        } catch (Exception e) {
            log.error("TossPay 빌링키 정기결제 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("빌링키 정기결제에 실패했습니다.", "BILLING_CHARGE_ERROR"));
        }
    }
}