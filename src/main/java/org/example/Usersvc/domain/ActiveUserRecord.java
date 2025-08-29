package org.example.Usersvc.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 활성 사용자 기록 엔티티 - DAU/MAU 측정을 위한 사용자 활동 기록
 */
@Entity
@Table(name = "active_user_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActiveUserRecord {
    
    @Id
    @Column(name = "record_id", length = 36)
    private String recordId; // UUID String
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType; // 활동 유형 (LOGIN, API_CALL, etc.)
    
    @Column(name = "active_date", nullable = false)
    private java.time.LocalDate activeDate; // 활동 날짜
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 생성 시간
    
    @Builder
    public ActiveUserRecord(String recordId, User user, String activityType, java.time.LocalDate activeDate) {
        this.recordId = recordId != null ? recordId : java.util.UUID.randomUUID().toString();
        this.user = user;
        this.activityType = activityType != null ? activityType : "API_CALL";
        this.activeDate = activeDate != null ? activeDate : java.time.LocalDate.now();
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.activeDate == null) {
            this.activeDate = java.time.LocalDate.now();
        }
        if (this.recordId == null) {
            this.recordId = java.util.UUID.randomUUID().toString();
        }
    }
    
    /**
     * 활동 유형 열거형
     */
    public enum ActivityType {
        LOGIN("LOGIN"),
        API_CALL("API_CALL"),
        PAGE_VIEW("PAGE_VIEW");
        
        private final String value;
        
        ActivityType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}