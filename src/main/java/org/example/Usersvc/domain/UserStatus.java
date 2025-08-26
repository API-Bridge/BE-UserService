package org.example.Usersvc.domain;

/**
 * 사용자 상태 enum
 * 
 * 사용자의 활성화 상태를 관리하기 위한 enum입니다.
 * API Gateway의 회원 탈퇴 기능과 연동하여 사용됩니다.
 */
public enum UserStatus {
    /**
     * 활성 상태 - 정상적으로 서비스를 이용할 수 있는 상태
     */
    ACTIVE("활성"),
    
    /**
     * 비활성화 상태 - 회원 탈퇴하여 서비스 이용이 중단된 상태 (복구 가능)
     */
    DEACTIVATED("비활성화"),
    
    /**
     * 일시 정지 상태 - 관리자에 의해 일시적으로 정지된 상태
     */
    SUSPENDED("일시정지");
    
    private final String description;
    
    UserStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 활성 상태인지 확인
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    /**
     * 비활성화 상태인지 확인
     */
    public boolean isDeactivated() {
        return this == DEACTIVATED;
    }
    
    /**
     * 정지 상태인지 확인
     */
    public boolean isSuspended() {
        return this == SUSPENDED;
    }
}