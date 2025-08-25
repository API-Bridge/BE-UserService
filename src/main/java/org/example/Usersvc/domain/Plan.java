package org.example.Usersvc.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 구독 플랜 엔티티 - plan 테이블과 매핑
 */
@Entity
@Table(name = "plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Plan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Integer planId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_name", nullable = false, unique = true)
    private PlanName planName;
    
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal price;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "features", columnDefinition = "JSON")
    private String features; // JSON 필드는 String으로 저장
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Builder
    public Plan(PlanName planName, java.math.BigDecimal price, String description, String features) {
        this.planName = planName;
        this.price = price;
        this.description = description;
        this.features = features;
    }
    
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 플랜 정보 업데이트
     */
    public void updatePlan(PlanName planName, java.math.BigDecimal price, String description, String features) {
        this.planName = planName;
        this.price = price;
        this.description = description;
        this.features = features;
    }
    
    /**
     * JSON features에서 값을 파싱하는 헬퍼 메서드들
     */
    public int getMaxApiCount() {
        return planName != null ? planName.getMaxApiCount() : 100;
    }
    
    public int getRateLimitPerMinute() {
        return planName != null ? planName.getRateLimitPerMinute() : 10;
    }
    
    public int getRateLimitPerHour() {
        return planName != null ? planName.getRateLimitPerHour() : 100;
    }
    
    public int getRateLimitPerDay() {
        return planName != null ? planName.getRateLimitPerDay() : 1000;
    }
    
    public int getMaxCustomApiCount() {
        return planName != null ? planName.getMaxCustomApiCount() : 5;
    }
    
    public int getMaxSharedApiCount() {
        return planName != null ? planName.getMaxSharedApiCount() : 3;
    }
    
    public int getMaxDataBundleCount() {
        return planName != null ? planName.getMaxDataBundleCount() : 3;
    }
    
    public String getPlanNameString() {
        return planName != null ? planName.getPlanName() : "Unknown";
    }
    
    public PlanName getPlanName() {
        return this.planName;
    }
    
    public java.math.BigDecimal getMonthlyPrice() {
        return this.price;
    }
    
    public java.math.BigDecimal getYearlyPrice() {
        return this.price.multiply(java.math.BigDecimal.valueOf(12));
    }
    
    private int getIntFromFeatures(String key, int defaultValue) {
        if (features == null || features.isEmpty()) {
            return defaultValue;
        }
        try {
            // 간단한 JSON 파싱 (실제로는 Jackson 사용 권장)
            String searchKey = "\"" + key + "\"";
            int keyIndex = features.indexOf(searchKey);
            if (keyIndex == -1) return defaultValue;
            
            int colonIndex = features.indexOf(":", keyIndex);
            if (colonIndex == -1) return defaultValue;
            
            int valueStart = colonIndex + 1;
            int valueEnd = features.indexOf(",", valueStart);
            if (valueEnd == -1) {
                valueEnd = features.indexOf("}", valueStart);
            }
            if (valueEnd == -1) return defaultValue;
            
            String valueStr = features.substring(valueStart, valueEnd).trim();
            return Integer.parseInt(valueStr);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
