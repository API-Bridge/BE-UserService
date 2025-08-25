package org.example.Usersvc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 커스텀 API 서비스용 통합 유저 정보 응답 DTO
 * 
 * 사용자의 기본 정보, 플랜 정보, 사용량 제한, 현재 사용량을 포함하는
 * 완전한 사용자 정보를 제공합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "통합 유저 정보 응답")
public class UserInfoResponse {

    @Schema(description = "사용자 ID", example = "user-001")
    private String userId;

    @Schema(description = "사용자 이메일", example = "user@example.com")
    private String userEmail;

    @Schema(description = "계정 생성 시간", example = "2024-01-15T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "플랜 정보")
    private PlanInfo planInfo;

    @Schema(description = "사용량 제한 정보")
    private UsageLimits usageLimits;

    @Schema(description = "현재 사용량 정보")
    private CurrentUsage currentUsage;

    /**
     * 사용자가 최근에 생성된 계정인지 확인
     * 
     * @param days 최근으로 간주할 일수
     * @return 지정된 일수 내에 생성된 계정인 경우 true
     */
    public boolean isRecentlyCreated(int days) {
        if (createdAt == null) {
            return false;
        }
        return createdAt.isAfter(LocalDateTime.now().minusDays(days));
    }

    /**
     * 사용자가 활성 구독을 가지고 있는지 확인
     * 
     * @return 활성 구독이 있는 경우 true
     */
    public boolean hasActiveSubscription() {
        return planInfo != null && planInfo.isActive();
    }

    /**
     * 사용자의 플랜 타입 반환
     * 
     * @return 플랜 타입 문자열 (FREE, PRO 등)
     */
    public String getPlanName() {
        return planInfo != null ? planInfo.getPlanName() : "FREE";
    }
}