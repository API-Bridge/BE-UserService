package org.example.Usersvc.event.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 구독 비활성화 이벤트
 * 
 * 사용자의 구독이 취소되거나 만료되어 비활성화되었을 때 발행되는 이벤트입니다.
 * 다른 마이크로서비스에서 구독 비활성화에 따른 후속 처리를 수행할 수 있도록 합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SubscriptionDeactivatedEvent extends BaseEvent {

    /**
     * 사용자 ID
     */
    private String userId;

    /**
     * 비활성화된 구독 ID
     */
    private String subscriptionId;

    /**
     * 비활성화된 플랜 ID
     */
    private Long planId;

    /**
     * 비활성화된 플랜 이름
     */
    private String planName;

    /**
     * Stripe 구독 ID
     */
    private String stripeSubscriptionId;

    /**
     * 구독 비활성화 시간
     */
    private LocalDateTime deactivatedAt;

    /**
     * 비활성화 사유 (CANCELLED, EXPIRED, PAYMENT_FAILED 등)
     */
    private String deactivationReason;

    /**
     * 사용자가 직접 취소했는지 여부
     */
    private Boolean isUserCancelled;

    /**
     * 구독이 활성화되었던 기간 (일수)
     */
    private Long activeDays;

    /**
     * 새로 전환될 플랜 타입 (보통 FREE)
     */
    private String newplanName;

    /**
     * 새로 생성된 구독 ID (FREE 플랜 전환 시)
     */
    private String newSubscriptionId;

    /**
     * 팩토리 메서드: 사용자 취소에 의한 구독 비활성화 이벤트 생성
     */
    public static SubscriptionDeactivatedEvent createUserCancelledEvent(String userId, 
            String subscriptionId, Long planId, String planName, 
            String stripeSubscriptionId, Long activeDays, String newSubscriptionId) {
        
        return SubscriptionDeactivatedEvent.builder()
                .eventType("SUBSCRIPTION_DEACTIVATED")
                .sourceService("user-service")
                .userId(userId)
                .subscriptionId(subscriptionId)
                .planId(planId)
                .planName(planName)
                .planName(planName)
                .stripeSubscriptionId(stripeSubscriptionId)
                .deactivatedAt(LocalDateTime.now())
                .deactivationReason("CANCELLED")
                .isUserCancelled(true)
                .activeDays(activeDays)
                .newplanName("FREE")
                .newSubscriptionId(newSubscriptionId)
                .build();
    }

    /**
     * 팩토리 메서드: 결제 실패에 의한 구독 비활성화 이벤트 생성
     */
    public static SubscriptionDeactivatedEvent createPaymentFailedEvent(String userId, 
            String subscriptionId, Long planId, String planName, 
            String stripeSubscriptionId, Long activeDays, String newSubscriptionId) {
        
        return SubscriptionDeactivatedEvent.builder()
                .eventType("SUBSCRIPTION_DEACTIVATED")
                .sourceService("user-service")
                .userId(userId)
                .subscriptionId(subscriptionId)
                .planId(planId)
                .planName(planName)
                .stripeSubscriptionId(stripeSubscriptionId)
                .deactivatedAt(LocalDateTime.now())
                .deactivationReason("PAYMENT_FAILED")
                .isUserCancelled(false)
                .activeDays(activeDays)
                .newplanName("FREE")
                .newSubscriptionId(newSubscriptionId)
                .build();
    }

    /**
     * 팩토리 메서드: 구독 만료에 의한 비활성화 이벤트 생성
     */
    public static SubscriptionDeactivatedEvent createExpiredEvent(String userId, 
            String subscriptionId, Long planId, String planName, 
            String stripeSubscriptionId, Long activeDays, String newSubscriptionId) {
        
        return SubscriptionDeactivatedEvent.builder()
                .eventType("SUBSCRIPTION_DEACTIVATED")
                .sourceService("user-service")
                .userId(userId)
                .subscriptionId(subscriptionId)
                .planId(planId)
                .planName(planName)
                .stripeSubscriptionId(stripeSubscriptionId)
                .deactivatedAt(LocalDateTime.now())
                .deactivationReason("EXPIRED")
                .isUserCancelled(false)
                .activeDays(activeDays)
                .newplanName("FREE")
                .newSubscriptionId(newSubscriptionId)
                .build();
    }

    /**
     * 팩토리 메서드: 관리자에 의한 구독 비활성화 이벤트 생성
     */
    public static SubscriptionDeactivatedEvent createAdminDeactivatedEvent(String userId, 
            String subscriptionId, Long planId, String planName, 
            String stripeSubscriptionId, Long activeDays, String newSubscriptionId) {
        
        return SubscriptionDeactivatedEvent.builder()
                .eventType("SUBSCRIPTION_DEACTIVATED")
                .sourceService("user-service")
                .userId(userId)
                .subscriptionId(subscriptionId)
                .planId(planId)
                .planName(planName)
                .stripeSubscriptionId(stripeSubscriptionId)
                .deactivatedAt(LocalDateTime.now())
                .deactivationReason("ADMIN_DEACTIVATED")
                .isUserCancelled(false)
                .activeDays(activeDays)
                .newplanName("FREE")
                .newSubscriptionId(newSubscriptionId)
                .build();
    }
}