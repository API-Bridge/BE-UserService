package org.example.Usersvc.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 구독 플랜 엔티티
 */
@Entity
@Table(name = "subscription_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;
    
    @Column(name = "plan_name", nullable = false, unique = true)
    private String planName;
    
    @Column(name = "max_api_count", nullable = false)
    private Integer maxApiCount; // 생성 가능한 최대 API 개수
    
    @Column(name = "rate_limit_per_minute", nullable = false) 
    private Integer rateLimitPerMinute; // 분당 호출 가능 횟수
    
    @Column(name = "rate_limit_per_hour", nullable = false)
    private Integer rateLimitPerHour; // 시간당 호출 가능 횟수
    
    @Column(name = "rate_limit_per_day", nullable = false)
    private Integer rateLimitPerDay; // 일일 호출 가능 횟수
    
    @Column(name = "monthly_price")
    private Integer monthlyPrice; // 월 구독료 (원)
    
    @Column(name = "yearly_price")
    private Integer yearlyPrice; // 연 구독료 (원)
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Builder
    public SubscriptionPlan(String planName, Integer maxApiCount, Integer rateLimitPerMinute, 
                           Integer rateLimitPerHour, Integer rateLimitPerDay, Integer monthlyPrice, Integer yearlyPrice) {
        this.planName = planName;
        this.maxApiCount = maxApiCount;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.rateLimitPerHour = rateLimitPerHour;
        this.rateLimitPerDay = rateLimitPerDay;
        this.monthlyPrice = monthlyPrice;
        this.yearlyPrice = yearlyPrice;
        this.isActive = true;
    }
    
    /**
     * 플랜 활성화/비활성화
     */
    public void updateStatus(boolean isActive) {
        this.isActive = isActive;
    }
}