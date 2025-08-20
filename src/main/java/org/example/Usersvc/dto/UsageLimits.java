package org.example.Usersvc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용량 제한 정보 DTO
 * 
 * 플랜별 사용량 제한 정보를 포함합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "사용량 제한 정보")
public class UsageLimits {

    @Schema(description = "최대 커스텀 API 개수", example = "5")
    private int maxCustomApiCount;

    @Schema(description = "최대 공유 API 개수", example = "3")
    private int maxSharedApiCount;

    @Schema(description = "최대 데이터 번들 개수", example = "10")
    private int maxDataBundleCount;

    @Schema(description = "분당 요청 제한", example = "10")
    private int rateLimitPerMinute;

    @Schema(description = "시간당 요청 제한", example = "100")
    private int rateLimitPerHour;

    @Schema(description = "일일 요청 제한", example = "1000")
    private int rateLimitPerDay;

    /**
     * 제한 정보가 유효한지 확인
     * 
     * @return 모든 제한값이 0 이상인 경우 true
     */
    public boolean isValid() {
        return maxCustomApiCount >= 0 &&
               maxSharedApiCount >= 0 &&
               maxDataBundleCount >= 0 &&
               rateLimitPerMinute >= 0 &&
               rateLimitPerHour >= 0 &&
               rateLimitPerDay >= 0;
    }

    /**
     * PRO 플랜 여부를 제한값으로 판단
     * 
     * @return PRO 플랜으로 추정되는 경우 true
     */
    public boolean isProbablyProPlan() {
        return maxCustomApiCount > 5 || 
               maxSharedApiCount > 3 ||
               rateLimitPerMinute > 10;
    }

    /**
     * 월간 예상 최대 요청 수 계산
     * 
     * @return 일일 제한 기준으로 월간 최대 요청 수
     */
    public long getMonthlyMaxRequests() {
        return (long) rateLimitPerDay * 30;
    }
}