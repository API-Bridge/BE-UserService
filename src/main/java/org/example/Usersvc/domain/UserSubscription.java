package org.example.Usersvc.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 구독 정보 엔티티 - 제공된 스키마에 맞게 수정
 */
@Entity
@Table(name = "subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSubscription {
    
    @Id
    @Column(name = "subscription_id", length = 36)
    private String subscriptionId; // UUID String으로 변경
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;
    
    @Column(name = "plan_payment_date", nullable = false)
    private LocalDateTime planPaymentDate; // started_at -> plan_payment_date
    
    @Column(name = "plan_update_date", nullable = false)
    private LocalDateTime planUpdateDate; // updated_at -> plan_update_date
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false; // 기본값이 FALSE
    
    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId; // Stripe 구독 ID
    
    @Builder
    public UserSubscription(String subscriptionId, User user, Plan plan, LocalDateTime planPaymentDate) {
        this.subscriptionId = subscriptionId != null ? subscriptionId : java.util.UUID.randomUUID().toString();
        this.user = user;
        this.plan = plan;
        this.planPaymentDate = planPaymentDate != null ? planPaymentDate : LocalDateTime.now();
        this.planUpdateDate = LocalDateTime.now();
        this.isActive = false; // 기본값은 false
    }
    
    @PreUpdate
    public void preUpdate() {
        this.planUpdateDate = LocalDateTime.now();
    }
    
    /**
     * 구독 만료 확인 - 새 스키마에서는 단순히 is_active 체크
     */
    public boolean isExpired() {
        return !isActive;
    }
    
    /**
     * 구독 취소
     */
    public void cancel() {
        this.isActive = false;
    }
    
    /**
     * 구독 갱신
     */
    public void renew() {
        this.planPaymentDate = LocalDateTime.now();
        this.planUpdateDate = LocalDateTime.now();
        this.isActive = true;
    }

    /**
     * 구독 활성화
     */
    public void activate() {
        this.isActive = true;
        this.planUpdateDate = LocalDateTime.now();
    }

    /**
     * 결제 연체 상태로 표시
     */
    public void markOverdue() {
        this.isActive = false;
        this.planUpdateDate = LocalDateTime.now();
    }

    /**
     * 결제 실패 상태로 표시
     */
    public void markPaymentFailed() {
        this.isActive = false;
        this.planUpdateDate = LocalDateTime.now();
    }

    /**
     * 결제일 업데이트
     */
    public void updatePaymentDate(LocalDateTime newPaymentDate) {
        this.planPaymentDate = newPaymentDate;
        this.planUpdateDate = LocalDateTime.now();
    }

    // Setter 메소드들 (웹훅 처리용)
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public void setPlanPaymentDate(LocalDateTime planPaymentDate) {
        this.planPaymentDate = planPaymentDate;
    }

    public void setPlanUpdateDate(LocalDateTime planUpdateDate) {
        this.planUpdateDate = planUpdateDate;
    }

    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Stripe 구독 ID 설정
     */
    public void setStripeSubscriptionId(String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }

}