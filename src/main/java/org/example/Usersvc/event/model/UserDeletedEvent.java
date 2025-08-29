package org.example.Usersvc.event.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 삭제 이벤트
 * 
 * 사용자가 시스템에서 삭제되었을 때 발행되는 이벤트입니다.
 * 다른 마이크로서비스들이 이 이벤트를 구독하여 
 * 사용자 삭제에 따른 정리 작업을 수행할 수 있습니다.
 * 
 * 이벤트 구독 서비스 예시:
 * - 파일 서비스: 사용자 파일 삭제
 * - 알림 서비스: 사용자 관련 알림 정리
 * - 분석 서비스: 사용자 삭제 통계 업데이트
 * - 권한 서비스: 사용자 권한 정리
 * - 결제 서비스: 구독 및 결제 정보 정리
 */
@Getter
@Builder
public class UserDeletedEvent extends BaseEvent {
    
    /**
     * 삭제된 사용자의 고유 식별자
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
     * 사용자 삭제 시간
     */
    private final LocalDateTime deletedAt;
    
    /**
     * 삭제 사유 (선택적)
     */
    private final String deletionReason;
    
    /**
     * 사용자 삭제 이벤트 생성자
     * 
     * @param userId 사용자 ID
     * @param auth0Id Auth0 사용자 ID
     * @param userEmail 사용자 이메일
     * @param deletedAt 삭제 시간
     * @param deletionReason 삭제 사유
     */
    public UserDeletedEvent(String userId, String auth0Id, String userEmail, 
                           LocalDateTime deletedAt, String deletionReason) {
        super("USER_DELETED");
        this.userId = userId;
        this.auth0Id = auth0Id;
        this.userEmail = userEmail;
        this.deletedAt = deletedAt;
        this.deletionReason = deletionReason;
    }

    @Override
    public Object getPayload() {
        return this;
    }
}