package org.example.Usersvc.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 플랜 제한 초과 예외
 * 
 * 사용자가 현재 플랜의 제한을 초과하여 API를 생성하거나 공유하려고 할 때 발생하는 예외입니다.
 * HTTP 403 Forbidden 상태로 응답됩니다.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class PlanLimitExceededException extends RuntimeException {

    private final String limitType;
    private final int currentCount;
    private final int maxAllowed;

    /**
     * 기본 생성자
     * 
     * @param message 예외 메시지
     */
    public PlanLimitExceededException(String message) {
        super(message);
        this.limitType = null;
        this.currentCount = 0;
        this.maxAllowed = 0;
    }

    /**
     * 상세 정보를 포함한 생성자
     * 
     * @param message 예외 메시지
     * @param limitType 제한 타입 (예: "CustomAPI", "SharedAPI")
     * @param currentCount 현재 사용량
     * @param maxAllowed 최대 허용량
     */
    public PlanLimitExceededException(String message, String limitType, int currentCount, int maxAllowed) {
        super(message);
        this.limitType = limitType;
        this.currentCount = currentCount;
        this.maxAllowed = maxAllowed;
    }

    /**
     * 커스텀 API 제한 초과 예외 생성
     * 
     * @param currentCount 현재 커스텀 API 개수
     * @param maxAllowed 최대 허용 개수
     * @return PlanLimitExceededException 인스턴스
     */
    public static PlanLimitExceededException forCustomApi(int currentCount, int maxAllowed) {
        String message = String.format("커스텀 API 제한을 초과했습니다. 현재: %d개, 최대: %d개", currentCount, maxAllowed);
        return new PlanLimitExceededException(message, "CustomAPI", currentCount, maxAllowed);
    }

    /**
     * 공유 API 제한 초과 예외 생성
     * 
     * @param currentCount 현재 공유 API 개수
     * @param maxAllowed 최대 허용 개수
     * @return PlanLimitExceededException 인스턴스
     */
    public static PlanLimitExceededException forSharedApi(int currentCount, int maxAllowed) {
        String message = String.format("공유 API 제한을 초과했습니다. 현재: %d개, 최대: %d개", currentCount, maxAllowed);
        return new PlanLimitExceededException(message, "SharedAPI", currentCount, maxAllowed);
    }

    /**
     * 요청 빈도 제한 초과 예외 생성
     * 
     * @param timeUnit 시간 단위 (예: "분", "시간", "일")
     * @param currentCount 현재 요청 수
     * @param maxAllowed 최대 허용 요청 수
     * @return PlanLimitExceededException 인스턴스
     */
    public static PlanLimitExceededException forRateLimit(String timeUnit, long currentCount, int maxAllowed) {
        String message = String.format("%s당 요청 제한을 초과했습니다. 현재: %d회, 최대: %d회", 
                                       timeUnit, currentCount, maxAllowed);
        return new PlanLimitExceededException(message, "RateLimit", (int) currentCount, maxAllowed);
    }

    /**
     * 플랜 업그레이드 권장 메시지 포함 예외 생성
     * 
     * @param baseMessage 기본 메시지
     * @param limitType 제한 타입
     * @param currentCount 현재 사용량
     * @param maxAllowed 최대 허용량
     * @return PlanLimitExceededException 인스턴스
     */
    public static PlanLimitExceededException withUpgradeRecommendation(
            String baseMessage, String limitType, int currentCount, int maxAllowed) {
        String fullMessage = baseMessage + " PRO 플랜으로 업그레이드하여 더 많은 기능을 이용하세요.";
        return new PlanLimitExceededException(fullMessage, limitType, currentCount, maxAllowed);
    }

    // Getters
    public String getLimitType() {
        return limitType;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public int getMaxAllowed() {
        return maxAllowed;
    }

    /**
     * 상세 정보가 있는지 확인
     * 
     * @return 제한 타입 정보가 있는 경우 true
     */
    public boolean hasDetailedInfo() {
        return limitType != null && !limitType.trim().isEmpty();
    }
}