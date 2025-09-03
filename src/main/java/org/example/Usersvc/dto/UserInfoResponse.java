package org.example.Usersvc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 커스텀 API 서비스용 통합 유저 정보 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "통합 유저 정보 응답")
public class UserInfoResponse {

    @Schema(description = "사용자 ID", example = "auth0|user123456")
    private String userId;

    @Schema(description = "사용자 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;

    @Schema(description = "플랜 정보", example = "PRO")
    private String plan;

    @Schema(description = "활성화 상태", example = "true")
    private Boolean isActive;

    @Schema(description = "계정 생성 시간", example = "2023-01-01T00:00:00Z")
    private LocalDateTime createdAt;

    @Schema(description = "계정 수정 시간", example = "2023-12-01T00:00:00Z")
    private LocalDateTime updatedAt;

}