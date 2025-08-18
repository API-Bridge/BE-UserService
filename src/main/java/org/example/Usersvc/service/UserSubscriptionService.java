package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.error.ErrorCode;
import org.example.Usersvc.common.exception.BusinessException;
import org.example.Usersvc.domain.SubscriptionPlan;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.repository.SubscriptionPlanRepository;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 사용자 구독 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSubscriptionService {
    
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    
    /**
     * 사용자의 활성 구독 조회
     */
    public UserSubscription getActiveSubscription(User user) {
        return userSubscriptionRepository.findActiveSubscriptionByUser(user)
                .orElseGet(() -> createDefaultSubscription(user));
    }
    
    /**
     * 구독 생성
     */
    @Transactional
    public UserSubscription createSubscription(User user, String planName, LocalDateTime expiresAt) {
        SubscriptionPlan plan = subscriptionPlanRepository.findByPlanName(planName)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_SUBSCRIPTION_PLAN));
        
        // 기존 활성 구독이 있다면 비활성화
        userSubscriptionRepository.findActiveSubscriptionByUser(user)
                .ifPresent(UserSubscription::cancel);
        
        UserSubscription subscription = UserSubscription.builder()
                .subscriptionId(java.util.UUID.randomUUID().toString())
                .user(user)
                .plan(plan)
                .planPaymentDate(LocalDateTime.now())
                .build();
        
        // 구독을 활성화
        subscription.activate();
        
        return userSubscriptionRepository.save(subscription);
    }
    
    /**
     * 구독 갱신
     */
    @Transactional
    public UserSubscription renewSubscription(User user) {
        UserSubscription subscription = getActiveSubscription(user);
        subscription.renew();
        return userSubscriptionRepository.save(subscription);
    }
    
    /**
     * 구독 취소
     */
    @Transactional
    public void cancelSubscription(User user) {
        UserSubscription subscription = getActiveSubscription(user);
        subscription.cancel();
        userSubscriptionRepository.save(subscription);
        
        // 무료 플랜으로 변경
        createDefaultSubscription(user);
    }
    
    /**
     * 기본 무료 구독 생성
     */
    @Transactional
    protected UserSubscription createDefaultSubscription(User user) {
        SubscriptionPlan freePlan = subscriptionPlanRepository.findFreePlan()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_SUBSCRIPTION_PLAN, "무료 플랜을 찾을 수 없습니다."));
        
        UserSubscription subscription = UserSubscription.builder()
                .subscriptionId(java.util.UUID.randomUUID().toString())
                .user(user)
                .plan(freePlan)
                .planPaymentDate(LocalDateTime.now())
                .build();
        
        // 구독을 활성화
        subscription.activate();
        
        log.info("Created default free subscription for user: {}", user.getUserId());
        return userSubscriptionRepository.save(subscription);
    }
    
    /**
     * 플랜 변경
     */
    @Transactional
    public UserSubscription changePlan(User user, String newPlanName) {
        SubscriptionPlan newPlan = subscriptionPlanRepository.findByPlanName(newPlanName)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_SUBSCRIPTION_PLAN));
        
        // 기존 구독 취소
        userSubscriptionRepository.findActiveSubscriptionByUser(user)
                .ifPresent(UserSubscription::cancel);
        
        // 새 구독 생성
        LocalDateTime expiresAt = newPlan.getPlanName().equals("FREE") ? 
                null : LocalDateTime.now().plusMonths(1);
        
        return createSubscription(user, newPlanName, expiresAt);
    }
    
    /**
     * 만료된 구독 처리
     */
    @Transactional
    public void processExpiredSubscriptions() {
        var expiredSubscriptions = userSubscriptionRepository.findInactiveSubscriptions();
        
        for (UserSubscription subscription : expiredSubscriptions) {
            log.info("Processing expired subscription for user: {}", subscription.getUser().getUserId());
            subscription.cancel();
            createDefaultSubscription(subscription.getUser());
        }
        
        if (!expiredSubscriptions.isEmpty()) {
            userSubscriptionRepository.saveAll(expiredSubscriptions);
            log.info("Processed {} expired subscriptions", expiredSubscriptions.size());
        }
    }
}