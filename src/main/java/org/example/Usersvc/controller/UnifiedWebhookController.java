package org.example.Usersvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.service.PaymentService;
import org.example.Usersvc.service.PaymentServiceFactory;
import org.example.Usersvc.service.WebhookRetryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 통합 웹훅 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class UnifiedWebhookController {

    private final PaymentServiceFactory paymentServiceFactory;
    private final WebhookRetryService webhookRetryService;

    /**
     * TossPay 웹훅 처리
     */
    @PostMapping("/tosspay")
    public ResponseEntity<ApiResponse<String>> handleTossPayWebhook(
            @RequestBody Map<String, Object> webhook) {
        
        log.info("TossPay 웹훅 수신: {}", webhook);

        try {
            // 재시도 로직이 포함된 웹훅 처리
            webhookRetryService.processWebhookWithRetry("TOSSPAY", webhook);
            
            log.info("✅ TossPay 웹훅 처리 완료");
            return ResponseEntity.ok(ApiResponse.success("웹훅 처리 완료", "WEBHOOK_SUCCESS"));

        } catch (Exception e) {
            log.error("TossPay 웹훅 처리 최종 실패: {}", e.getMessage(), e);
            // 최종 실패 처리
            webhookRetryService.handleWebhookFailure("TOSSPAY", webhook, e);
            
            // 웹훅은 성공 응답을 반환해야 무한 재시도를 방지함
            return ResponseEntity.ok(ApiResponse.success("웹훅 수신 완료", "WEBHOOK_RECEIVED"));
        }
    }

    /**
     * Stripe 웹훅 처리 (향후 구현)
     */
    @PostMapping("/stripe")
    public ResponseEntity<ApiResponse<String>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        log.info("Stripe 웹훅은 현재 비활성화되어 있습니다.");
        
        return ResponseEntity.ok(ApiResponse.success("Stripe 웹훅은 비활성화되어 있습니다.", "STRIPE_DISABLED"));
    }
}