package org.example.Usersvc.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.example.Usersvc.common.logging.UserActionLogger;
import org.example.Usersvc.common.metrics.CustomMetrics;
import org.example.Usersvc.event.model.SubscriptionUpdatedEvent;
import org.example.Usersvc.event.model.SubscriptionDeactivatedEvent;
import org.example.Usersvc.event.publisher.EventPublisherService;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeSubscriptionService {
    
    private final MockStripeService mockStripeService;
    private final UserActionLogger userActionLogger;
    private final CustomMetrics customMetrics;
    private final EventPublisherService eventPublisher;

    public String createSubscription(String customerId, String priceId) {
        // Stripe API 키 유효성 검사
        String currentApiKey = com.stripe.Stripe.apiKey;
        if (!mockStripeService.isValidStripeKey(currentApiKey)) {
            log.warn("유효하지 않은 Stripe API 키 감지, Mock 서비스 사용");
            return mockStripeService.createMockSubscription(customerId, priceId);
        }
        
        try {
            SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                    .setCustomer(customerId)
                    .addItem(
                            SubscriptionCreateParams.Item.builder()
                                    .setPrice(priceId)
                                    .build()
                    )
                    .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                    .addExpand("latest_invoice.payment_intent")
                    .build();

            Subscription subscription = Subscription.create(params);
            log.info("Stripe 구독 생성 완료 - subscriptionId: {}, customerId: {}, priceId: {}", 
                    subscription.getId(), customerId, priceId);
            return subscription.getId();
        } catch (StripeException e) {
            log.error("Stripe 구독 생성 실패 - customerId: {}, priceId: {}, error: {}", 
                    customerId, priceId, e.getMessage());
            log.info("실제 Stripe API 실패, Mock 서비스로 전환");
            return mockStripeService.createMockSubscription(customerId, priceId);
        }
    }

    public String updateSubscription(String subscriptionId, String newPriceId, String userId, String oldPlanType, String newPlanType) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .addItem(
                            SubscriptionUpdateParams.Item.builder()
                                    .setId(subscription.getItems().getData().get(0).getId())
                                    .setPrice(newPriceId)
                                    .build()
                    )
                    .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
                    .build();

            Subscription updatedSubscription = subscription.update(params);
            
            // 카프카 이벤트 발행
            if (userId != null) {
                SubscriptionUpdatedEvent event = SubscriptionUpdatedEvent.createUpgradeEvent(
                    userId, subscriptionId, null, newPlanType, newPlanType, oldPlanType, 
                    updatedSubscription.getId(), 0.0, "USD");
                eventPublisher.publishEvent("subscription.updated", event);
                
                // 메트릭 기록
                customMetrics.recordConversionRate(oldPlanType, newPlanType);
            }
            
            log.info("Stripe 구독 업데이트 완료 - subscriptionId: {}, newPriceId: {}", 
                    subscriptionId, newPriceId);
            return updatedSubscription.getId();
        } catch (StripeException e) {
            log.error("Stripe 구독 업데이트 실패 - subscriptionId: {}, newPriceId: {}, error: {}", 
                    subscriptionId, newPriceId, e.getMessage());
            throw new RuntimeException("Stripe 구독 업데이트 실패: " + e.getMessage(), e);
        }
    }

    public boolean cancelSubscription(String subscriptionId, String userId, String planType, String reason) {
        // Mock 구독 ID인지 확인 또는 Stripe API 키 유효성 검사
        if (subscriptionId.startsWith("sub_mock_") || !mockStripeService.isValidStripeKey(com.stripe.Stripe.apiKey)) {
            log.warn("Mock 구독 또는 유효하지 않은 Stripe API 키 감지, Mock 서비스 사용");
            return mockStripeService.cancelMockSubscription(subscriptionId);
        }
        
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            Subscription canceledSubscription = subscription.cancel();
            
            boolean isCanceled = "canceled".equals(canceledSubscription.getStatus());
            
            // 카프카 이벤트 발행
            if (userId != null && isCanceled) {
                SubscriptionDeactivatedEvent event = SubscriptionDeactivatedEvent.createUserCancelledEvent(
                    userId, subscriptionId, null, planType, planType, subscriptionId, 0L, null);
                eventPublisher.publishEvent("subscription.deactivated", event);
                
                // 메트릭 기록
                customMetrics.incrementSubscriptionCancelled(planType);
            }
            
            log.info("Stripe 구독 취소 완료 - subscriptionId: {}, status: {}", 
                    subscriptionId, canceledSubscription.getStatus());
            return isCanceled;
        } catch (StripeException e) {
            log.error("Stripe 구독 취소 실패 - subscriptionId: {}, error: {}", subscriptionId, e.getMessage());
            log.info("실제 Stripe API 실패, Mock 서비스로 전환");
            return mockStripeService.cancelMockSubscription(subscriptionId);
        }
    }

    public Subscription getSubscription(String subscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            log.debug("Stripe 구독 조회 완료 - subscriptionId: {}", subscriptionId);
            return subscription;
        } catch (StripeException e) {
            log.error("Stripe 구독 조회 실패 - subscriptionId: {}, error: {}", subscriptionId, e.getMessage());
            throw new RuntimeException("Stripe 구독 조회 실패: " + e.getMessage(), e);
        }
    }

    public boolean pauseSubscription(String subscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setPauseCollection(
                            SubscriptionUpdateParams.PauseCollection.builder()
                                    .setBehavior(SubscriptionUpdateParams.PauseCollection.Behavior.VOID)
                                    .build()
                    )
                    .build();

            Subscription pausedSubscription = subscription.update(params);
            log.info("Stripe 구독 일시정지 완료 - subscriptionId: {}", subscriptionId);
            return true;
        } catch (StripeException e) {
            log.error("Stripe 구독 일시정지 실패 - subscriptionId: {}, error: {}", subscriptionId, e.getMessage());
            throw new RuntimeException("Stripe 구독 일시정지 실패: " + e.getMessage(), e);
        }
    }

    public boolean resumeSubscription(String subscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setPauseCollection((SubscriptionUpdateParams.PauseCollection) null)
                    .build();

            Subscription resumedSubscription = subscription.update(params);
            log.info("Stripe 구독 재개 완료 - subscriptionId: {}", subscriptionId);
            return true;
        } catch (StripeException e) {
            log.error("Stripe 구독 재개 실패 - subscriptionId: {}, error: {}", subscriptionId, e.getMessage());
            throw new RuntimeException("Stripe 구독 재개 실패: " + e.getMessage(), e);
        }
    }
}