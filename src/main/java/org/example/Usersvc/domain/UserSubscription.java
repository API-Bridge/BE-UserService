package org.example.Usersvc.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 구독 정보 엔티티
 */
@Entity
@Table(name = "user_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSubscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long subscriptionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;
    
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Builder
    public UserSubscription(User user, SubscriptionPlan plan, LocalDateTime startedAt, LocalDateTime expiresAt) {
        this.user = user;
        this.plan = plan;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 구독 만료 확인
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
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
    public void renew(LocalDateTime newExpiresAt) {
        this.expiresAt = newExpiresAt;
        this.isActive = true;
    }

    /**
     * 구독 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 결제 연체 상태로 표시
     */
    public void markOverdue() {
        // 현재는 상태만 비활성화, 향후 별도 상태 필드 추가 가능
        this.isActive = false;
    }

    /**
     * 결제 실패 상태로 표시
     */
    public void markPaymentFailed() {
        // 현재는 상태만 비활성화, 향후 별도 상태 필드 추가 가능
        this.isActive = false;
    }

    /**
     * 만료일 업데이트
     */
    public void updateExpirationDate(LocalDateTime newExpiresAt) {
        this.expiresAt = newExpiresAt;
    }
}