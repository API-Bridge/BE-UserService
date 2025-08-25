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
    
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider", length = 20)
    private PaymentProvider paymentProvider = PaymentProvider.TOSSPAY; // 결제 제공자
    
    @Column(name = "billing_key")
    private String billingKey; // TossPay 빌링키 (정기결제용)
    
    @Builder
    public UserSubscription(String subscriptionId, User user, Plan plan, LocalDateTime planPaymentDate) {
        this.subscriptionId = subscriptionId != null ? subscriptionId : java.util.UUID.randomUUID().toString();
        this.user = user;
        this.plan = plan;
        this.planPaymentDate = planPaymentDate != null ? planPaymentDate : LocalDateTime.now();
        this.planUpdateDate = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.planUpdateDate = LocalDateTime.now();
    }
    
    /**
     * 구독이 활성 상태인지 확인 - plan_id로 판단
     */
    public boolean isActive() {
        return this.plan != null;
    }
    
    
    /**
     * 구독 갱신
     */
    public void renew() {
        this.planPaymentDate = LocalDateTime.now();
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

    public void setPlan(Plan plan) {
        this.plan = plan;
        this.planUpdateDate = LocalDateTime.now();
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
     * 결제 제공자 설정
     */
    public void setPaymentProvider(PaymentProvider paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    /**
     * 빌링키 설정 (TossPay)
     */
    public void setBillingKey(String billingKey) {
        this.billingKey = billingKey;
    }

}