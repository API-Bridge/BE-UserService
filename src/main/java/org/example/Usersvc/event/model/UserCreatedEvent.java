package org.example.Usersvc.event.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 생성 이벤트
 * 
 * 새로운 사용자가 시스템에 등록되었을 때 발행되는 이벤트입니다.
 * 다른 마이크로서비스들이 이 이벤트를 구독하여 
 * 사용자 생성에 따른 후속 처리를 수행할 수 있습니다.
 * 
 * 이벤트 구독 서비스 예시:
 * - 메일 서비스: 환영 메일 발송
 * - 알림 서비스: 가입 완료 알림
 * - 분석 서비스: 사용자 가입 통계 업데이트
 * - 권한 서비스: 기본 권한 할당
 */
@Getter
@Builder
public class UserCreatedEvent extends BaseEvent {
    
    /**
     * 생성된 사용자의 고유 식별자
     */
    private final String userId;
    
    /**
     * Auth0에서 제공하는 사용자 식별자
     */
    private final String auth0Id;
    
    /**
     * 사용자 이메일 주소
     */
    private final String userEmail;
    
    /**
     * 사용자 생성 시간
     */
    private final LocalDateTime createdAt;
    
    /**
     * 사용자 생성 이벤트 생성자
     * 
     * @param userId 사용자 ID
     * @param auth0Id Auth0 사용자 ID
     * @param userEmail 사용자 이메일
     * @param createdAt 생성 시간
     */
    public UserCreatedEvent(String userId, String auth0Id, String userEmail, LocalDateTime createdAt) {
        super("USER_CREATED");
        this.userId = userId;
        this.auth0Id = auth0Id;
        this.userEmail = userEmail;
        this.createdAt = createdAt;
    }
}