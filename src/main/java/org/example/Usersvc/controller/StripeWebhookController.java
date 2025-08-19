package org.example.Usersvc.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.service.StripeWebhookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Stripe 웹훅 처리 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    /**
     * Stripe 웹훅 엔드포인트
     * 
     * @param request HTTP 요청 (Stripe 서명 헤더 포함)
     * @param payload 웹훅 페이로드
     * @return 처리 결과
     */
    @PostMapping
    public ResponseEntity<ApiResponse<String>> handleWebhook(
            HttpServletRequest request,
            @RequestBody String payload) {

        String sigHeader = request.getHeader("Stripe-Signature");
        Event event;

        try {
            // Stripe 서명 검증
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("웹훅 이벤트 수신: {}", event.getType());
        } catch (SignatureVerificationException e) {
            log.error("웹훅 서명 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("웹훅 서명 검증 실패", "WEBHOOK_SIGNATURE_INVALID"));
        }

        try {
            // 이벤트 타입별 처리
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                case "customer.subscription.created":
                    handleSubscriptionCreated(event);
                    break;
                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;
                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(event);
                    break;
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event);
                    break;
                default:
                    log.info("처리하지 않는 웹훅 이벤트: {}", event.getType());
                    break;
            }

            return ResponseEntity.ok(ApiResponse.success("웹훅 처리 완료", "OK"));
            
        } catch (Exception e) {
            log.error("웹훅 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("웹훅 처리 실패", "WEBHOOK_PROCESSING_ERROR"));
        }
    }

    /**
     * 체크아웃 세션 완료 처리
     */
    private void handleCheckoutSessionCompleted(Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session != null) {
                log.info("체크아웃 세션 완료: session_id={}, customer={}, subscription={}", 
                    session.getId(), session.getCustomer(), session.getSubscription());
                
                stripeWebhookService.processCheckoutCompleted(session);
            }
        } catch (Exception e) {
            log.error("체크아웃 세션 완료 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 구독 생성 처리
     */
    private void handleSubscriptionCreated(Event event) {
        try {
            com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) 
                event.getDataObjectDeserializer().getObject().orElse(null);
            if (subscription != null) {
                log.info("구독 생성: subscription_id={}, customer={}, status={}", 
                    subscription.getId(), subscription.getCustomer(), subscription.getStatus());
                
                stripeWebhookService.processSubscriptionCreated(subscription);
            }
        } catch (Exception e) {
            log.error("구독 생성 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 구독 업데이트 처리
     */
    private void handleSubscriptionUpdated(Event event) {
        try {
            com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) 
                event.getDataObjectDeserializer().getObject().orElse(null);
            if (subscription != null) {
                log.info("구독 업데이트: subscription_id={}, customer={}, status={}", 
                    subscription.getId(), subscription.getCustomer(), subscription.getStatus());
                
                stripeWebhookService.processSubscriptionUpdated(subscription);
            }
        } catch (Exception e) {
            log.error("구독 업데이트 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 구독 삭제 처리
     */
    private void handleSubscriptionDeleted(Event event) {
        try {
            com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) 
                event.getDataObjectDeserializer().getObject().orElse(null);
            if (subscription != null) {
                log.info("구독 삭제: subscription_id={}, customer={}", 
                    subscription.getId(), subscription.getCustomer());
                
                stripeWebhookService.processSubscriptionDeleted(subscription);
            }
        } catch (Exception e) {
            log.error("구독 삭제 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 결제 성공 처리
     */
    private void handleInvoicePaymentSucceeded(Event event) {
        try {
            com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) 
                event.getDataObjectDeserializer().getObject().orElse(null);
            if (invoice != null) {
                log.info("결제 성공: invoice_id={}, customer={}, subscription={}", 
                    invoice.getId(), invoice.getCustomer(), invoice.getSubscription());
                
                stripeWebhookService.processPaymentSucceeded(invoice);
            }
        } catch (Exception e) {
            log.error("결제 성공 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 결제 실패 처리
     */
    private void handleInvoicePaymentFailed(Event event) {
        try {
            com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) 
                event.getDataObjectDeserializer().getObject().orElse(null);
            if (invoice != null) {
                log.info("결제 실패: invoice_id={}, customer={}, subscription={}", 
                    invoice.getId(), invoice.getCustomer(), invoice.getSubscription());
                
                stripeWebhookService.processPaymentFailed(invoice);
            }
        } catch (Exception e) {
            log.error("결제 실패 처리 실패: {}", e.getMessage(), e);
        }
    }
}