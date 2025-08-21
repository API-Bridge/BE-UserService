package org.example.Usersvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.domain.PlanType;
import org.example.Usersvc.service.TossPayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * TossPay 결제 컨트롤러
 * 
 * ⚠️ DEPRECATED: UnifiedPaymentController로 대체됨
 * 레거시 호환성을 위해 일부 엔드포인트만 유지
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
            @RequestBody Map<String, Object> request) {
        
        String userId = (String) request.get("userId");
        String planTypeStr = (String) request.get("planType");
        String provider = (String) request.getOrDefault("provider", "TOSSPAY");
        
        log.info("결제 요청 - userId: {}, planType: {}, provider: {}", userId, planTypeStr, provider);
        
        try {
            PlanType planType = PlanType.valueOf(planTypeStr);
            Map<String, Object> paymentData = tossPayService.createSubscriptionPayment(userId, planType);
            
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
     * 빌링키 등록 (정기결제용)
     */
    @PostMapping("/billing/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerBilling(
            @RequestBody Map<String, Object> request) {
        
        String userId = (String) request.get("userId");
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
}