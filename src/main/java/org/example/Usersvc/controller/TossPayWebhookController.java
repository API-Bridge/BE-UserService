package org.example.Usersvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.service.TossPayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * TossPay 웹훅 컨트롤러
 * 
 * ⚠️ DEPRECATED: UnifiedWebhookController로 대체됨
 * 레거시 호환성을 위해 일부 엔드포인트만 유지
 */
@Slf4j
@RestController
@RequestMapping("/api/tosspay/webhook")
@RequiredArgsConstructor
public class TossPayWebhookController {

    private final TossPayService tossPayService;

    /**
     * 결제 승인 웹훅
     */
    @PostMapping("/payment")
    public ResponseEntity<ApiResponse<String>> paymentWebhook(@RequestBody Map<String, Object> webhook) {
        log.info("TossPay 결제 웹훅 수신: {}", webhook);

        try {
            String eventType = (String) webhook.get("eventType");
            Map<String, Object> data = (Map<String, Object>) webhook.get("data");

            if ("Payment".equals(eventType) && data != null) {
                String paymentKey = (String) data.get("paymentKey");
                String orderId = (String) data.get("orderId");
                Integer amount = (Integer) data.get("totalAmount");
                String status = (String) data.get("status");

                log.info("TossPay 결제 웹훅 처리 - paymentKey: {}, orderId: {}, amount: {}, status: {}", 
                    paymentKey, orderId, amount, status);

                if ("DONE".equals(status)) {
                    tossPayService.confirmPayment(paymentKey, orderId, amount);
                    log.info("✅ TossPay 결제 승인 완료 - orderId: {}", orderId);
                }
            }

            return ResponseEntity.ok(ApiResponse.success("웹훅 처리 완료", "WEBHOOK_SUCCESS"));

        } catch (Exception e) {
            log.error("TossPay 웹훅 처리 실패: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success("웹훅 수신 완료", "WEBHOOK_RECEIVED"));
        }
    }

    /**
     * 빌링키 등록 웹훅
     */
    @PostMapping("/billing")
    public ResponseEntity<ApiResponse<String>> billingWebhook(@RequestBody Map<String, Object> webhook) {
        log.info("TossPay 빌링키 웹훅 수신: {}", webhook);

        try {
            String eventType = (String) webhook.get("eventType");
            Map<String, Object> data = (Map<String, Object>) webhook.get("data");

            if ("BillingKey".equals(eventType) && data != null) {
                String billingKey = (String) data.get("billingKey");
                String customerKey = (String) data.get("customerKey");
                String status = (String) data.get("authenticatedAt");

                log.info("TossPay 빌링키 웹훅 처리 - billingKey: {}, customerKey: {}, status: {}", 
                    billingKey, customerKey, status);

                // 빌링키 저장 로직 - 운영 환경에서는 암호화하여 저장 필요
            }

            return ResponseEntity.ok(ApiResponse.success("빌링키 웹훅 처리 완료", "BILLING_WEBHOOK_SUCCESS"));

        } catch (Exception e) {
            log.error("TossPay 빌링키 웹훅 처리 실패: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success("빌링키 웹훅 수신 완료", "BILLING_WEBHOOK_RECEIVED"));
        }
    }
}