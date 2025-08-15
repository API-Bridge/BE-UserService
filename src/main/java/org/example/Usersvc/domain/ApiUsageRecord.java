package org.example.Usersvc.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API 사용량 기록 엔티티
 */
@Entity
@Table(name = "api_usage_records", 
       indexes = {
           @Index(name = "idx_user_date", columnList = "user_id, record_date"),
           @Index(name = "idx_user_api_date", columnList = "user_id, api_endpoint, record_date")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiUsageRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "api_endpoint", nullable = false)
    private String apiEndpoint; // 호출된 API 엔드포인트
    
    @Column(name = "request_method", nullable = false)
    private String requestMethod; // HTTP 메소드 (GET, POST 등)
    
    @Column(name = "response_status", nullable = false)
    private Integer responseStatus; // HTTP 응답 상태 코드
    
    @Column(name = "response_time_ms")
    private Long responseTimeMs; // 응답 시간 (밀리초)
    
    @Column(name = "request_ip")
    private String requestIp; // 요청자 IP 주소
    
    @Column(name = "user_agent")
    private String userAgent; // 사용자 에이전트
    
    @Column(name = "record_date", nullable = false)
    private LocalDateTime recordDate; // 기록 날짜
    
    @Builder
    public ApiUsageRecord(User user, String apiEndpoint, String requestMethod, 
                         Integer responseStatus, Long responseTimeMs, 
                         String requestIp, String userAgent) {
        this.user = user;
        this.apiEndpoint = apiEndpoint;
        this.requestMethod = requestMethod;
        this.responseStatus = responseStatus;
        this.responseTimeMs = responseTimeMs;
        this.requestIp = requestIp;
        this.userAgent = userAgent;
        this.recordDate = LocalDateTime.now();
    }
    
    /**
     * 성공적인 API 호출인지 확인
     */
    public boolean isSuccessful() {
        return responseStatus >= 200 && responseStatus < 300;
    }
}