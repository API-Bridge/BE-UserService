package org.example.Usersvc.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeSubscriptionService {

    public String createSubscription(String customerId, String priceId) {
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
            throw new RuntimeException("Stripe 구독 생성 실패: " + e.getMessage(), e);
        }
    }

    public String updateSubscription(String subscriptionId, String newPriceId) {
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
            log.info("Stripe 구독 업데이트 완료 - subscriptionId: {}, newPriceId: {}", 
                    subscriptionId, newPriceId);
            return updatedSubscription.getId();
        } catch (StripeException e) {
            log.error("Stripe 구독 업데이트 실패 - subscriptionId: {}, newPriceId: {}, error: {}", 
                    subscriptionId, newPriceId, e.getMessage());
            throw new RuntimeException("Stripe 구독 업데이트 실패: " + e.getMessage(), e);
        }
    }

    public boolean cancelSubscription(String subscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            Subscription canceledSubscription = subscription.cancel();
            
            boolean isCanceled = "canceled".equals(canceledSubscription.getStatus());
            log.info("Stripe 구독 취소 완료 - subscriptionId: {}, status: {}", 
                    subscriptionId, canceledSubscription.getStatus());
            return isCanceled;
        } catch (StripeException e) {
            log.error("Stripe 구독 취소 실패 - subscriptionId: {}, error: {}", subscriptionId, e.getMessage());
            throw new RuntimeException("Stripe 구독 취소 실패: " + e.getMessage(), e);
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