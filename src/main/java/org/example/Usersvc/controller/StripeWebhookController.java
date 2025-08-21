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
@RequestMapping("/api/webhook")
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
    @PostMapping("/stripe")
    public ResponseEntity<ApiResponse<String>> handleWebhook(
            HttpServletRequest request,
            @RequestBody String payload) {

        log.info("=== Stripe 웹훅 요청 수신 ===");
        log.info("Request URL: {}", request.getRequestURL());
        log.info("Content Type: {}", request.getContentType());
        log.info("Payload length: {}", payload != null ? payload.length() : 0);
        
        String sigHeader = request.getHeader("Stripe-Signature");
        log.info("Stripe-Signature header: {}", sigHeader != null ? "있음" : "없음");
        log.info("현재 설정된 webhook secret: {}", webhookSecret.substring(0, 10) + "...");
        
        Event event;

        try {
            // Stripe 서명 검증
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("✅ 웹훅 서명 검증 성공 - 이벤트 타입: {}", event.getType());
        } catch (SignatureVerificationException e) {
            log.error("❌ 웹훅 서명 검증 실패: {}", e.getMessage());
            log.error("Expected webhook secret starts with: {}", webhookSecret.substring(0, 10) + "...");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("웹훅 서명 검증 실패", "WEBHOOK_SIGNATURE_INVALID"));
        } catch (Exception e) {
            log.error("❌ 웹훅 파싱 중 예외 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("웹훅 파싱 실패", "WEBHOOK_PARSING_ERROR"));
        }

        try {
            // 이벤트 타입별 처리
            switch (event.getType()) {
                case "checkout.session.completed":
                    log.info("🔥 checkout.session.completed 이벤트 처리 시작");
                    handleCheckoutSessionCompleted(event);
                    log.info("🔥 checkout.session.completed 이벤트 처리 완료");
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
                    log.info("✅ 처리하지 않는 웹훅 이벤트 (정상): {}", event.getType());
                    log.debug("이벤트 세부 정보: {}", event.toJson());
                    break;
            }

            log.info("✅ 웹훅 처리 완료: {}", event.getType());
            return ResponseEntity.ok(ApiResponse.success("웹훅 처리 완료", "OK"));
            
        } catch (Exception e) {
            log.error("❌ 웹훅 처리 중 오류 발생 - 이벤트: {}, 오류: {}", 
                event != null ? event.getType() : "Unknown", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("웹훅 처리 실패", "WEBHOOK_PROCESSING_ERROR"));
        }
    }

    /**
     * 체크아웃 세션 완료 처리
     */
    private void handleCheckoutSessionCompleted(Event event) {
        try {
            log.info("🔧 Event 데이터 분석 시작");
            log.info("🔧 Event ID: {}", event.getId());
            log.info("🔧 Event Type: {}", event.getType());
            log.info("🔧 Event API Version: {}", event.getApiVersion());
            
            // 이벤트 데이터 구조 확인
            var dataObjectDeserializer = event.getDataObjectDeserializer();
            log.info("🔧 DataObjectDeserializer present: {}", dataObjectDeserializer != null);
            
            if (dataObjectDeserializer != null) {
                var objectOptional = dataObjectDeserializer.getObject();
                log.info("🔧 Object present: {}", objectOptional.isPresent());
                
                if (objectOptional.isPresent()) {
                    var object = objectOptional.get();
                    log.info("🔧 Object class: {}", object.getClass().getName());
                    log.info("🔧 Object toString: {}", object.toString());
                    
                    // Session으로 캐스팅 시도
                    if (object instanceof Session) {
                        Session session = (Session) object;
                        log.info("✅ Session 캐스팅 성공!");
                        log.info("체크아웃 세션 완료: session_id={}, customer={}, subscription={}, metadata={}", 
                            session.getId(), session.getCustomer(), session.getSubscription(), session.getMetadata());
                        
                        log.info("StripeWebhookService 호출 시작 - processCheckoutCompleted");
                        stripeWebhookService.processCheckoutCompleted(session);
                        log.info("StripeWebhookService 호출 완료 - processCheckoutCompleted");
                    } else {
                        log.error("❌ 객체가 Session 타입이 아닙니다: {}", object.getClass().getName());
                        
                        // Raw JSON을 통해 Session 생성 시도
                        try {
                            log.info("🔧 Raw JSON으로 Session 생성 시도");
                            String rawJson = event.getData().getObject().toJson();
                            log.info("🔧 Raw JSON: {}", rawJson);
                            log.error("❌ Session 객체 파싱에 실패했지만 raw JSON은 확인됨");
                        } catch (Exception jsonEx) {
                            log.error("❌ Raw JSON 처리 실패: {}", jsonEx.getMessage(), jsonEx);
                        }
                    }
                } else {
                    log.error("❌ Event에서 객체를 추출할 수 없습니다.");
                }
            } else {
                log.error("❌ DataObjectDeserializer가 null입니다.");
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
            log.info("🔧 구독 생성 이벤트 처리 시작");
            var dataObjectDeserializer = event.getDataObjectDeserializer();
            log.info("🔧 DataObjectDeserializer present: {}", dataObjectDeserializer != null);
            
            if (dataObjectDeserializer != null) {
                var objectOptional = dataObjectDeserializer.getObject();
                log.info("🔧 Object present: {}", objectOptional.isPresent());
                
                if (objectOptional.isPresent()) {
                    var object = objectOptional.get();
                    log.info("🔧 Object class: {}", object.getClass().getName());
                    
                    if (object instanceof com.stripe.model.Subscription) {
                        com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) object;
                        log.info("✅ Subscription 캐스팅 성공!");
                        log.info("구독 생성: subscription_id={}, customer={}, status={}", 
                            subscription.getId(), subscription.getCustomer(), subscription.getStatus());
                        
                        log.info("StripeWebhookService 호출 시작 - processSubscriptionCreated");
                        stripeWebhookService.processSubscriptionCreated(subscription);
                        log.info("StripeWebhookService 호출 완료 - processSubscriptionCreated");
                    } else {
                        log.error("❌ 객체가 Subscription 타입이 아닙니다: {}", object.getClass().getName());
                        
                        // Raw JSON 로깅만 수행
                        try {
                            log.info("🔧 Raw JSON으로 Subscription 정보 확인");
                            String rawJson = event.getData().getObject().toJson();
                            log.info("🔧 Raw JSON: {}", rawJson);
                            log.error("❌ Subscription 객체 파싱에 실패했지만 raw JSON은 확인됨");
                        } catch (Exception jsonEx) {
                            log.error("❌ Raw JSON 처리 실패: {}", jsonEx.getMessage(), jsonEx);
                        }
                    }
                } else {
                    log.error("❌ Event에서 객체를 추출할 수 없습니다.");
                }
            } else {
                log.error("❌ DataObjectDeserializer가 null입니다.");
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
            log.info("🔧 구독 업데이트 이벤트 처리 시작");
            var dataObjectDeserializer = event.getDataObjectDeserializer();
            
            if (dataObjectDeserializer != null) {
                var objectOptional = dataObjectDeserializer.getObject();
                
                if (objectOptional.isPresent()) {
                    var object = objectOptional.get();
                    
                    if (object instanceof com.stripe.model.Subscription) {
                        com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) object;
                        log.info("✅ Subscription 업데이트 캐스팅 성공!");
                        log.info("구독 업데이트: subscription_id={}, customer={}, status={}", 
                            subscription.getId(), subscription.getCustomer(), subscription.getStatus());
                        
                        stripeWebhookService.processSubscriptionUpdated(subscription);
                    } else {
                        // Raw JSON 로깅만 수행
                        try {
                            String rawJson = event.getData().getObject().toJson();
                            log.info("🔧 Subscription 업데이트 Raw JSON: {}", rawJson);
                            log.error("❌ Subscription 업데이트 객체 파싱에 실패했지만 raw JSON은 확인됨");
                        } catch (Exception jsonEx) {
                            log.error("❌ Raw JSON 처리 실패: {}", jsonEx.getMessage(), jsonEx);
                        }
                    }
                }
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