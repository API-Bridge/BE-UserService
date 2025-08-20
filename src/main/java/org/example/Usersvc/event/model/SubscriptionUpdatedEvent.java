package org.example.Usersvc.event.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 구독 업데이트 이벤트
 * 
 * 사용자의 구독 플랜이 변경되었을 때 발행되는 이벤트입니다.
 * 다른 마이크로서비스에서 구독 변경에 따른 후속 처리를 수행할 수 있도록 합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SubscriptionUpdatedEvent extends BaseEvent {

    /**
     * 사용자 ID
     */
    private String userId;

    /**
     * 구독 ID
     */
    private String subscriptionId;

    /**
     * 플랜 ID
     */
    private Long planId;

    /**
     * 플랜 타입 (FREE, PRO)
     */
    private String planType;

    /**
     * 플랜 이름
     */
    private String planName;

    /**
     * 구독 활성 상태
     */
    private Boolean isActive;

    /**
     * 이전 플랜 타입 (변경 전)
     */
    private String previousPlanType;

    /**
     * Stripe 구독 ID
     */
    private String stripeSubscriptionId;

    /**
     * 구독 업데이트 시간
     */
    private LocalDateTime updatedAt;

    /**
     * 결제 금액
     */
    private Double amount;

    /**
     * 통화 코드 (USD, KRW 등)
     */
    private String currency;

    /**
     * 업데이트 사유 (UPGRADE, DOWNGRADE, RENEWAL 등)
     */
    private String updateReason;

    /**
     * 팩토리 메서드: 플랜 업그레이드 이벤트 생성
     */
    public static SubscriptionUpdatedEvent createUpgradeEvent(String userId, String subscriptionId, 
            Long planId, String planType, String planName, String previousPlanType, 
            String stripeSubscriptionId, Double amount, String currency) {
        
        return SubscriptionUpdatedEvent.builder()
                .eventType("SUBSCRIPTION_UPDATED")
                .sourceService("user-service")
                .userId(userId)
                .subscriptionId(subscriptionId)
                .planId(planId)
                .planType(planType)
                .planName(planName)
                .isActive(true)
                .previousPlanType(previousPlanType)
                .stripeSubscriptionId(stripeSubscriptionId)
                .updatedAt(LocalDateTime.now())
                .amount(amount)
                .currency(currency)
                .updateReason("UPGRADE")
                .build();
    }

    /**
     * 팩토리 메서드: 구독 갱신 이벤트 생성
     */
    public static SubscriptionUpdatedEvent createRenewalEvent(String userId, String subscriptionId, 
            Long planId, String planType, String planName, String stripeSubscriptionId, 
            Double amount, String currency) {
        
        return SubscriptionUpdatedEvent.builder()
                .eventType("SUBSCRIPTION_UPDATED")
                .sourceService("user-service")
                .userId(userId)
                .subscriptionId(subscriptionId)
                .planId(planId)
                .planType(planType)
                .planName(planName)
                .isActive(true)
                .previousPlanType(planType) // 갱신의 경우 이전 플랜과 동일
                .stripeSubscriptionId(stripeSubscriptionId)
                .updatedAt(LocalDateTime.now())
                .amount(amount)
                .currency(currency)
                .updateReason("RENEWAL")
                .build();
    }

    /**
     * 팩토리 메서드: FREE 플랜 전환 이벤트 생성 (구독 취소 후)
     */
    public static SubscriptionUpdatedEvent createFreePlanConversionEvent(String userId, 
            String subscriptionId, Long planId, String previousPlanType) {
        
        return SubscriptionUpdatedEvent.builder()
                .eventType("SUBSCRIPTION_UPDATED")
                .sourceService("user-service")
                .userId(userId)
                .subscriptionId(subscriptionId)
                .planId(planId)
                .planType("FREE")
                .planName("Free Plan")
                .isActive(true)
                .previousPlanType(previousPlanType)
                .stripeSubscriptionId(null)
                .updatedAt(LocalDateTime.now())
                .amount(0.0)
                .currency("USD")
                .updateReason("DOWNGRADE")
                .build();
    }
}