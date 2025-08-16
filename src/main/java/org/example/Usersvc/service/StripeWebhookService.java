package org.example.Usersvc.service;

import com.stripe.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {

    private final UserRepository userRepository;
    private final SubscriptionManagementService subscriptionManagementService;

    public void handleWebhook(Event event) {
        log.info("Stripe 웹훅 처리 시작 - eventType: {}, eventId: {}", event.getType(), event.getId());

        switch (event.getType()) {
            case "customer.subscription.created":
            case "customer.subscription.updated":
                handleSubscriptionEvent(event);
                break;
            case "customer.subscription.deleted":
                handleSubscriptionDeleted(event);
                break;
            case "invoice.payment_succeeded":
                handlePaymentSucceeded(event);
                break;
            case "invoice.payment_failed":
                handlePaymentFailed(event);
                break;
            case "payment_method.attached":
                handlePaymentMethodAttached(event);
                break;
            case "customer.updated":
                handleCustomerUpdated(event);
                break;
            default:
                log.debug("지원하지 않는 이벤트 타입 - eventType: {}", event.getType());
                break;
        }

        log.info("Stripe 웹훅 처리 완료 - eventType: {}, eventId: {}", event.getType(), event.getId());
    }

    private void handleSubscriptionEvent(Event event) {
        try {
            Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (subscription == null) {
                log.warn("구독 이벤트 객체를 파싱할 수 없음 - eventId: {}", event.getId());
                return;
            }

            String customerId = subscription.getCustomer();

            User user = findUserByStripeCustomerId(customerId);
            if (user == null) {
                log.warn("Stripe 고객 ID로 사용자를 찾을 수 없음 - customerId: {}", customerId);
                return;
            }

            log.info("구독 상태 변경 처리 - userId: {}, subscriptionId: {}, status: {}", 
                    user.getUserId(), subscription.getId(), subscription.getStatus());

            subscriptionManagementService.handleSubscriptionStatusChange(user, subscription);
        } catch (Exception e) {
            log.error("구독 이벤트 처리 중 오류 발생 - eventId: {}, error: {}", event.getId(), e.getMessage());
        }
    }

    private void handleSubscriptionDeleted(Event event) {
        try {
            Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (subscription == null) {
                log.warn("구독 삭제 이벤트 객체를 파싱할 수 없음 - eventId: {}", event.getId());
                return;
            }

            String customerId = subscription.getCustomer();

            User user = findUserByStripeCustomerId(customerId);
            if (user == null) {
                log.warn("Stripe 고객 ID로 사용자를 찾을 수 없음 - customerId: {}", customerId);
                return;
            }

            log.info("구독 취소 처리 - userId: {}, subscriptionId: {}", 
                    user.getUserId(), subscription.getId());

            subscriptionManagementService.handleSubscriptionCancellation(user, subscription.getId());
        } catch (Exception e) {
            log.error("구독 삭제 이벤트 처리 중 오류 발생 - eventId: {}, error: {}", event.getId(), e.getMessage());
        }
    }

    private void handlePaymentSucceeded(Event event) {
        try {
            Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (invoice == null) {
                log.warn("결제 성공 이벤트 객체를 파싱할 수 없음 - eventId: {}", event.getId());
                return;
            }

            String customerId = invoice.getCustomer();
            String subscriptionId = invoice.getSubscription();
            Long amountPaid = invoice.getAmountPaid();

            User user = findUserByStripeCustomerId(customerId);
            if (user == null) {
                log.warn("Stripe 고객 ID로 사용자를 찾을 수 없음 - customerId: {}", customerId);
                return;
            }

            log.info("결제 성공 처리 - userId: {}, subscriptionId: {}, amountPaid: {}", 
                    user.getUserId(), subscriptionId, amountPaid);

            subscriptionManagementService.handlePaymentSuccess(user, subscriptionId, amountPaid);
        } catch (Exception e) {
            log.error("결제 성공 이벤트 처리 중 오류 발생 - eventId: {}, error: {}", event.getId(), e.getMessage());
        }
    }

    private void handlePaymentFailed(Event event) {
        try {
            Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (invoice == null) {
                log.warn("결제 실패 이벤트 객체를 파싱할 수 없음 - eventId: {}", event.getId());
                return;
            }

            String customerId = invoice.getCustomer();
            String subscriptionId = invoice.getSubscription();
            Long amountDue = invoice.getAmountDue();

            User user = findUserByStripeCustomerId(customerId);
            if (user == null) {
                log.warn("Stripe 고객 ID로 사용자를 찾을 수 없음 - customerId: {}", customerId);
                return;
            }

            log.info("결제 실패 처리 - userId: {}, subscriptionId: {}, amountDue: {}", 
                    user.getUserId(), subscriptionId, amountDue);

            subscriptionManagementService.handlePaymentFailure(user, subscriptionId, amountDue);
        } catch (Exception e) {
            log.error("결제 실패 이벤트 처리 중 오류 발생 - eventId: {}, error: {}", event.getId(), e.getMessage());
        }
    }

    private void handlePaymentMethodAttached(Event event) {
        try {
            PaymentMethod paymentMethod = (PaymentMethod) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (paymentMethod == null) {
                log.warn("결제 수단 연결 이벤트 객체를 파싱할 수 없음 - eventId: {}", event.getId());
                return;
            }

            String customerId = paymentMethod.getCustomer();

            User user = findUserByStripeCustomerId(customerId);
            if (user == null) {
                log.warn("Stripe 고객 ID로 사용자를 찾을 수 없음 - customerId: {}", customerId);
                return;
            }

            log.info("결제 수단 연결 처리 - userId: {}, paymentMethodId: {}", 
                    user.getUserId(), paymentMethod.getId());

            subscriptionManagementService.handlePaymentMethodAttached(user, paymentMethod.getId());
        } catch (Exception e) {
            log.error("결제 수단 연결 이벤트 처리 중 오류 발생 - eventId: {}, error: {}", event.getId(), e.getMessage());
        }
    }

    private void handleCustomerUpdated(Event event) {
        try {
            Customer customer = (Customer) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (customer == null) {
                log.warn("고객 업데이트 이벤트 객체를 파싱할 수 없음 - eventId: {}", event.getId());
                return;
            }

            String customerId = customer.getId();

            User user = findUserByStripeCustomerId(customerId);
            if (user == null) {
                log.warn("Stripe 고객 ID로 사용자를 찾을 수 없음 - customerId: {}", customerId);
                return;
            }

            log.info("고객 정보 업데이트 처리 - userId: {}, customerId: {}", 
                    user.getUserId(), customerId);

            subscriptionManagementService.handleCustomerUpdated(user, customer);
        } catch (Exception e) {
            log.error("고객 업데이트 이벤트 처리 중 오류 발생 - eventId: {}, error: {}", event.getId(), e.getMessage());
        }
    }

    private User findUserByStripeCustomerId(String customerId) {
        return userRepository.findByStripeCustomerId(customerId).orElse(null);
    }
}