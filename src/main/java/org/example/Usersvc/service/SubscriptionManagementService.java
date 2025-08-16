package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.config.StripeProperties;
import org.example.Usersvc.domain.SubscriptionPlan;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionManagementService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanManagementService planManagementService;
    private final StripeSubscriptionService stripeSubscriptionService;
    private final StripeCustomerService stripeCustomerService;
    private final StripeProperties stripeProperties;

    public UserSubscription createSubscription(User user, String planName, String billingPeriod) {
        log.info("구독 생성 시작 - userId: {}, planName: {}, billingPeriod: {}", 
                user.getUserId(), planName, billingPeriod);

        SubscriptionPlan plan = planManagementService.getPlanByName(planName)
                .orElseThrow(() -> new IllegalArgumentException("플랜을 찾을 수 없습니다: " + planName));

        // 기존 활성 구독 취소
        cancelExistingActiveSubscription(user);

        UserSubscription newSubscription;

        if ("FREE".equals(planName)) {
            // 무료 플랜은 Stripe 구독 없이 생성
            newSubscription = createFreeSubscription(user, plan);
        } else {
            // 유료 플랜은 Stripe 구독과 함께 생성
            newSubscription = createPaidSubscription(user, plan, billingPeriod);
        }

        UserSubscription savedSubscription = userSubscriptionRepository.save(newSubscription);
        log.info("구독 생성 완료 - userId: {}, planName: {}, subscriptionId: {}", 
                user.getUserId(), planName, savedSubscription.getSubscriptionId());

        return savedSubscription;
    }

    public UserSubscription changeSubscription(User user, String stripeSubscriptionId, String newPlanName, String billingPeriod) {
        log.info("구독 변경 시작 - userId: {}, stripeSubscriptionId: {}, newPlanName: {}", 
                user.getUserId(), stripeSubscriptionId, newPlanName);

        UserSubscription currentSubscription = userSubscriptionRepository.findActiveSubscriptionByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("활성 구독을 찾을 수 없습니다."));

        SubscriptionPlan newPlan = planManagementService.getPlanByName(newPlanName)
                .orElseThrow(() -> new IllegalArgumentException("플랜을 찾을 수 없습니다: " + newPlanName));

        // 현재 구독 취소
        currentSubscription.cancel();
        userSubscriptionRepository.save(currentSubscription);

        if ("FREE".equals(newPlanName)) {
            // 유료에서 무료로 변경: Stripe 구독 취소
            stripeSubscriptionService.cancelSubscription(stripeSubscriptionId);
            return createFreeSubscription(user, newPlan);
        } else {
            // 유료 플랜 변경: Stripe 구독 업데이트
            String newPriceId = getBillingPeriod(billingPeriod).equals("YEARLY") 
                    ? stripeProperties.getYearlyPriceIdByPlanType(newPlanName)
                    : stripeProperties.getMonthlyPriceIdByPlanType(newPlanName);

            String updatedStripeSubscriptionId = stripeSubscriptionService.updateSubscription(stripeSubscriptionId, newPriceId);
            
            UserSubscription newSubscription = UserSubscription.builder()
                    .user(user)
                    .plan(newPlan)
                    .startedAt(LocalDateTime.now())
                    .expiresAt(calculateExpirationDate(billingPeriod))
                    .build();

            return userSubscriptionRepository.save(newSubscription);
        }
    }

    public void cancelSubscription(User user, String stripeSubscriptionId) {
        log.info("구독 취소 시작 - userId: {}, stripeSubscriptionId: {}", user.getUserId(), stripeSubscriptionId);

        UserSubscription activeSubscription = userSubscriptionRepository.findActiveSubscriptionByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("활성 구독을 찾을 수 없습니다."));

        // Stripe 구독 취소
        if (stripeSubscriptionId != null) {
            stripeSubscriptionService.cancelSubscription(stripeSubscriptionId);
        }

        // 로컬 구독 취소
        activeSubscription.cancel();
        userSubscriptionRepository.save(activeSubscription);

        // 무료 플랜으로 자동 변경
        SubscriptionPlan freePlan = planManagementService.getFreePlan();
        UserSubscription freeSubscription = createFreeSubscription(user, freePlan);
        userSubscriptionRepository.save(freeSubscription);

        log.info("구독 취소 완료 - userId: {}, 무료 플랜으로 변경됨", user.getUserId());
    }

    @Transactional(readOnly = true)
    public UserSubscription getCurrentSubscription(User user) {
        return userSubscriptionRepository.findActiveSubscriptionByUser(user)
                .orElseGet(() -> {
                    log.warn("활성 구독을 찾을 수 없음, 무료 플랜으로 생성 - userId: {}", user.getUserId());
                    SubscriptionPlan freePlan = planManagementService.getFreePlan();
                    return createFreeSubscription(user, freePlan);
                });
    }

    private void cancelExistingActiveSubscription(User user) {
        Optional<UserSubscription> existingSubscription = userSubscriptionRepository.findActiveSubscriptionByUser(user);
        if (existingSubscription.isPresent()) {
            existingSubscription.get().cancel();
            userSubscriptionRepository.save(existingSubscription.get());
            log.info("기존 활성 구독 취소 완료 - userId: {}", user.getUserId());
        }
    }

    private UserSubscription createFreeSubscription(User user, SubscriptionPlan plan) {
        return UserSubscription.builder()
                .user(user)
                .plan(plan)
                .startedAt(LocalDateTime.now())
                .expiresAt(null) // 무료 플랜은 만료일 없음
                .build();
    }

    private UserSubscription createPaidSubscription(User user, SubscriptionPlan plan, String billingPeriod) {
        // Stripe 고객 생성 또는 조회
        String customerId = getOrCreateStripeCustomer(user);

        // 가격 ID 결정
        String priceId = getBillingPeriod(billingPeriod).equals("YEARLY") 
                ? stripeProperties.getYearlyPriceIdByPlanType(plan.getPlanName())
                : stripeProperties.getMonthlyPriceIdByPlanType(plan.getPlanName());

        // Stripe 구독 생성
        String stripeSubscriptionId = stripeSubscriptionService.createSubscription(customerId, priceId);

        return UserSubscription.builder()
                .user(user)
                .plan(plan)
                .startedAt(LocalDateTime.now())
                .expiresAt(calculateExpirationDate(billingPeriod))
                .build();
    }

    private String getOrCreateStripeCustomer(User user) {
        String existingCustomerId = stripeCustomerService.findCustomerByEmail(user.getUserEmail());
        if (existingCustomerId != null) {
            log.debug("기존 Stripe 고객 사용 - customerId: {}, userId: {}", existingCustomerId, user.getUserId());
            return existingCustomerId;
        } else {
            String newCustomerId = stripeCustomerService.createCustomer(user);
            log.info("새 Stripe 고객 생성 - customerId: {}, userId: {}", newCustomerId, user.getUserId());
            return newCustomerId;
        }
    }

    private LocalDateTime calculateExpirationDate(String billingPeriod) {
        if ("YEARLY".equals(getBillingPeriod(billingPeriod))) {
            return LocalDateTime.now().plusYears(1);
        } else {
            return LocalDateTime.now().plusMonths(1);
        }
    }

    private String getBillingPeriod(String billingPeriod) {
        return billingPeriod != null ? billingPeriod.toUpperCase() : "MONTHLY";
    }

    public void handleSubscriptionStatusChange(User user, com.stripe.model.Subscription stripeSubscription) {
        log.info("Stripe 구독 상태 변경 처리 - userId: {}, subscriptionId: {}, status: {}", 
                user.getUserId(), stripeSubscription.getId(), stripeSubscription.getStatus());

        Optional<UserSubscription> currentSubscription = userSubscriptionRepository.findActiveSubscriptionByUser(user);
        
        switch (stripeSubscription.getStatus()) {
            case "active":
                handleActiveSubscription(user, stripeSubscription, currentSubscription);
                break;
            case "past_due":
            case "unpaid":
                handleOverdueSubscription(user, stripeSubscription, currentSubscription);
                break;
            case "canceled":
            case "incomplete_expired":
                handleCanceledSubscription(user, stripeSubscription, currentSubscription);
                break;
            default:
                log.debug("처리하지 않는 구독 상태 - status: {}", stripeSubscription.getStatus());
                break;
        }
    }

    public void handleSubscriptionCancellation(User user, String stripeSubscriptionId) {
        log.info("Stripe 구독 취소 처리 - userId: {}, subscriptionId: {}", user.getUserId(), stripeSubscriptionId);
        
        Optional<UserSubscription> activeSubscription = userSubscriptionRepository.findActiveSubscriptionByUser(user);
        if (activeSubscription.isPresent()) {
            activeSubscription.get().cancel();
            userSubscriptionRepository.save(activeSubscription.get());
        }

        SubscriptionPlan freePlan = planManagementService.getFreePlan();
        UserSubscription freeSubscription = createFreeSubscription(user, freePlan);
        userSubscriptionRepository.save(freeSubscription);
    }

    public void handlePaymentSuccess(User user, String subscriptionId, Long amountPaid) {
        log.info("결제 성공 처리 - userId: {}, subscriptionId: {}, amountPaid: {}", 
                user.getUserId(), subscriptionId, amountPaid);

        Optional<UserSubscription> subscription = userSubscriptionRepository.findActiveSubscriptionByUser(user);
        if (subscription.isPresent()) {
            UserSubscription userSubscription = subscription.get();
            String billingPeriod = determineBillingPeriod(amountPaid, userSubscription.getPlan());
            LocalDateTime newExpiresAt = calculateExpirationDate(billingPeriod);
            
            userSubscription.updateExpirationDate(newExpiresAt);
            userSubscriptionRepository.save(userSubscription);
        }
    }

    public void handlePaymentFailure(User user, String subscriptionId, Long amountDue) {
        log.warn("결제 실패 처리 - userId: {}, subscriptionId: {}, amountDue: {}", 
                user.getUserId(), subscriptionId, amountDue);

        Optional<UserSubscription> subscription = userSubscriptionRepository.findActiveSubscriptionByUser(user);
        if (subscription.isPresent()) {
            UserSubscription userSubscription = subscription.get();
            userSubscription.markPaymentFailed();
            userSubscriptionRepository.save(userSubscription);
        }
    }

    public void handlePaymentMethodAttached(User user, String paymentMethodId) {
        log.info("결제 수단 연결 처리 - userId: {}, paymentMethodId: {}", user.getUserId(), paymentMethodId);
    }

    public void handleCustomerUpdated(User user, com.stripe.model.Customer customer) {
        log.info("Stripe 고객 정보 업데이트 처리 - userId: {}, customerId: {}", user.getUserId(), customer.getId());
    }

    private void handleActiveSubscription(User user, com.stripe.model.Subscription stripeSubscription, 
                                        Optional<UserSubscription> currentSubscription) {
        if (currentSubscription.isPresent()) {
            UserSubscription subscription = currentSubscription.get();
            subscription.activate();
            userSubscriptionRepository.save(subscription);
        }
    }

    private void handleOverdueSubscription(User user, com.stripe.model.Subscription stripeSubscription, 
                                         Optional<UserSubscription> currentSubscription) {
        if (currentSubscription.isPresent()) {
            UserSubscription subscription = currentSubscription.get();
            subscription.markOverdue();
            userSubscriptionRepository.save(subscription);
        }
    }

    private void handleCanceledSubscription(User user, com.stripe.model.Subscription stripeSubscription, 
                                          Optional<UserSubscription> currentSubscription) {
        if (currentSubscription.isPresent()) {
            currentSubscription.get().cancel();
            userSubscriptionRepository.save(currentSubscription.get());
        }

        SubscriptionPlan freePlan = planManagementService.getFreePlan();
        UserSubscription freeSubscription = createFreeSubscription(user, freePlan);
        userSubscriptionRepository.save(freeSubscription);
    }

    private String determineBillingPeriod(Long amountPaid, org.example.Usersvc.domain.SubscriptionPlan plan) {
        if (plan.getYearlyPrice() != null && amountPaid.equals(plan.getYearlyPrice().longValue())) {
            return "YEARLY";
        }
        return "MONTHLY";
    }
}