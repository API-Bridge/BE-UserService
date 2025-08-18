package org.example.Usersvc.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API 사용량 기록 엔티티 - 새 스키마에 맞게 수정
 */
@Entity
@Table(name = "api_usage_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiUsageRecord {
    
    @Id
    @Column(name = "record_id", length = 36)
    private String recordId; // UUID String으로 변경
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "api_endpoint", nullable = false)
    private String apiEndpoint; // 호출된 API 엔드포인트
    
    @Column(name = "request_count", nullable = false)
    private Integer requestCount; // 요청 횟수 (새 스키마에 맞게 변경)
    
    @Column(name = "record_date", nullable = false)
    private java.time.LocalDate recordDate; // 기록 날짜 (LocalDate로 변경)
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 생성 시간
    
    @Builder
    public ApiUsageRecord(String recordId, User user, String apiEndpoint, Integer requestCount, java.time.LocalDate recordDate) {
        this.recordId = recordId != null ? recordId : java.util.UUID.randomUUID().toString();
        this.user = user;
        this.apiEndpoint = apiEndpoint;
        this.requestCount = requestCount != null ? requestCount : 1;
        this.recordDate = recordDate != null ? recordDate : java.time.LocalDate.now();
        this.createdAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.recordDate == null) {
            this.recordDate = java.time.LocalDate.now();
        }
        if (this.recordId == null) {
            this.recordId = java.util.UUID.randomUUID().toString();
        }
    }
    
    /**
     * 요청 횟수 증가
     */
    public void incrementRequestCount(int count) {
        this.requestCount += count;
    }
}