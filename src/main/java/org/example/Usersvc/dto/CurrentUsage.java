package org.example.Usersvc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 현재 사용량 정보 DTO
 * 
 * 사용자의 현재 API 사용량 및 생성한 API 개수 정보를 포함합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "현재 사용량 정보")
public class CurrentUsage {

    @Schema(description = "현재 커스텀 API 개수", example = "2")
    private int customApiCount;

    @Schema(description = "현재 공유 API 개수", example = "1")
    private int sharedApiCount;

    @Schema(description = "저장된 API 개수", example = "3")
    private int savedApiCount;

    @Schema(description = "분당 현재 사용량", example = "5")
    private long minuteUsage;

    @Schema(description = "시간당 현재 사용량", example = "45")
    private long hourUsage;

    @Schema(description = "일일 현재 사용량", example = "320")
    private long dayUsage;

    /**
     * 커스텀 API 제한 초과 여부 확인
     * 
     * @param maxLimit 최대 허용 개수
     * @return 제한을 초과한 경우 true
     */
    public boolean isCustomApiLimitExceeded(int maxLimit) {
        return customApiCount >= maxLimit;
    }

    /**
     * 공유 API 제한 초과 여부 확인
     * 
     * @param maxLimit 최대 허용 개수
     * @return 제한을 초과한 경우 true
     */
    public boolean isSharedApiLimitExceeded(int maxLimit) {
        return sharedApiCount >= maxLimit;
    }

    /**
     * 분당 요청 제한 초과 여부 확인
     * 
     * @param maxLimit 최대 허용 요청 수
     * @return 제한을 초과한 경우 true
     */
    public boolean isMinuteRateLimitExceeded(int maxLimit) {
        return minuteUsage >= maxLimit;
    }

    /**
     * 시간당 요청 제한 초과 여부 확인
     * 
     * @param maxLimit 최대 허용 요청 수
     * @return 제한을 초과한 경우 true
     */
    public boolean isHourRateLimitExceeded(int maxLimit) {
        return hourUsage >= maxLimit;
    }

    /**
     * 일일 요청 제한 초과 여부 확인
     * 
     * @param maxLimit 최대 허용 요청 수
     * @return 제한을 초과한 경우 true
     */
    public boolean isDayRateLimitExceeded(int maxLimit) {
        return dayUsage >= maxLimit;
    }

    /**
     * 전체 사용률 계산 (0.0 ~ 1.0)
     * 
     * @param limits 사용량 제한 정보
     * @return 평균 사용률
     */
    public double getOverallUsageRatio(UsageLimits limits) {
        if (limits == null || !limits.isValid()) {
            return 0.0;
        }

        double apiUsageRatio = (double) (customApiCount + sharedApiCount) / 
                              (limits.getMaxCustomApiCount() + limits.getMaxSharedApiCount());
        double requestUsageRatio = (double) dayUsage / limits.getRateLimitPerDay();

        return Math.min(1.0, (apiUsageRatio + requestUsageRatio) / 2.0);
    }

    /**
     * 남은 커스텀 API 생성 가능 개수
     * 
     * @param maxLimit 최대 허용 개수
     * @return 남은 생성 가능 개수
     */
    public int getRemainingCustomApiCount(int maxLimit) {
        return Math.max(0, maxLimit - customApiCount);
    }

    /**
     * 남은 공유 API 생성 가능 개수
     * 
     * @param maxLimit 최대 허용 개수
     * @return 남은 생성 가능 개수
     */
    public int getRemainingSharedApiCount(int maxLimit) {
        return Math.max(0, maxLimit - sharedApiCount);
    }
}