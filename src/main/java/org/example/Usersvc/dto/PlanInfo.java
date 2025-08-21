package org.example.Usersvc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 플랜 정보 DTO
 * 
 * 사용자의 현재 구독 플랜 정보를 포함합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "사용자 플랜 정보")
public class PlanInfo {

    @Schema(description = "플랜 타입", example = "FREE", allowableValues = {"FREE", "PRO"})
    private String planType;

    @Schema(description = "플랜 이름", example = "Free Plan")
    private String planName;

    @Schema(description = "구독 활성 상태", example = "true")
    private boolean isActive;

    @Schema(description = "플랜 결제 시작일", example = "2024-01-15T10:00:00")
    private LocalDateTime planPaymentDate;

    @Schema(description = "플랜 업데이트일", example = "2024-01-15T10:00:00")
    private LocalDateTime planUpdateDate;

    @Schema(description = "결제 제공자", example = "TOSSPAY", allowableValues = {"TOSSPAY", "FREE"})
    private String paymentProvider;

    @Schema(description = "플랜 가격", example = "0.00")
    private Double price;

    @Schema(description = "플랜 설명", example = "무료 플랜 - 시작하기에 완벽")
    private String description;

    /**
     * 유료 플랜인지 확인
     * 
     * @return PRO 플랜인 경우 true
     */
    public boolean isPaidPlan() {
        return "PRO".equals(planType);
    }

    /**
     * TossPay 결제인지 확인
     * 
     * @return TossPay 결제인 경우 true
     */
    public boolean hasTossPaySubscription() {
        return "TOSSPAY".equals(paymentProvider);
    }

    /**
     * 플랜이 유효한지 확인
     * 
     * @return 필수 정보가 모두 있는 경우 true
     */
    public boolean isValid() {
        return planType != null && !planType.trim().isEmpty() &&
               planName != null && !planName.trim().isEmpty() &&
               planPaymentDate != null;
    }
}