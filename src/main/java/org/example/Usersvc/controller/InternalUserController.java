package org.example.Usersvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 내부 API 전용 사용자 관리 컨트롤러
 * 
 * API Gateway에서 호출하는 내부 서비스 전용 엔드포인트를 제공합니다.
 * 외부에서 직접 접근하지 않으며, 마이크로서비스 간 통신용으로만 사용됩니다.
 * 
 * 주요 기능:
 * - 사용자 비활성화 (회원 탈퇴)
 * - 사용자 완전 삭제 (GDPR 요구사항)
 * - 사용자 활성화 (계정 복구)
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal User API", description = "내부 서비스 간 통신용 사용자 관리 API")
public class InternalUserController {

    private final UserService userService;

    /**
     * 사용자 비활성화 (회원 탈퇴)
     * 
     * API Gateway의 회원 탈퇴 기능에서 호출됩니다.
     * 사용자 상태를 DEACTIVATED로 변경하여 서비스 이용을 중단시키지만,
     * 데이터는 보존하여 복구가 가능합니다.
     * 
     * @param userId 비활성화할 사용자 ID
     * @return 처리 결과
     */
    @PatchMapping("/{userId}/deactivate")
    @Operation(summary = "사용자 비활성화", description = "사용자를 비활성화 상태로 변경합니다 (복구 가능)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 비활성화 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<org.example.Usersvc.common.response.ApiResponse> deactivateUser(
            @Parameter(description = "사용자 ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId) {
        
        log.info("Internal API: Deactivating user {}", userId);
        
        try {
            boolean success = userService.deactivateUser(userId);
            
            if (success) {
                log.info("Successfully deactivated user: {}", userId);
                return ResponseEntity.ok(
                    org.example.Usersvc.common.response.ApiResponse.success("사용자가 성공적으로 비활성화되었습니다.", "DEACTIVATED")
                );
            } else {
                log.warn("Failed to deactivate user: {} - User not found", userId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Error deactivating user: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(org.example.Usersvc.common.response.ApiResponse.error("사용자 비활성화 중 오류가 발생했습니다.", e.getMessage()));
        }
    }

    /**
     * 사용자 완전 삭제
     * 
     * API Gateway의 계정 완전 삭제 기능에서 호출됩니다.
     * GDPR 요구사항이나 명시적 완전 삭제 요청 시 사용됩니다.
     * 주의: 복구 불가능한 완전 삭제입니다.
     * 
     * @param userId 삭제할 사용자 ID
     * @return 처리 결과
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "사용자 완전 삭제", description = "사용자 데이터를 완전히 삭제합니다 (복구 불가능)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<org.example.Usersvc.common.response.ApiResponse> deleteUser(
            @Parameter(description = "사용자 ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId) {
        
        log.warn("Internal API: Permanently deleting user {}", userId);
        
        try {
            boolean success = userService.deleteUser(userId);
            
            if (success) {
                log.warn("Successfully deleted user: {}", userId);
                return ResponseEntity.ok(
                    org.example.Usersvc.common.response.ApiResponse.success("사용자가 완전히 삭제되었습니다.", "DELETED")
                );
            } else {
                log.warn("Failed to delete user: {} - User not found", userId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Error deleting user: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(org.example.Usersvc.common.response.ApiResponse.error("사용자 삭제 중 오류가 발생했습니다.", e.getMessage()));
        }
    }

    /**
     * 사용자 활성화 (계정 복구)
     * 
     * API Gateway의 계정 복구 기능에서 호출됩니다.
     * 비활성화된 사용자를 다시 활성 상태로 복구합니다.
     * 
     * @param userId 활성화할 사용자 ID
     * @return 처리 결과
     */
    @PatchMapping("/{userId}/activate")
    @Operation(summary = "사용자 활성화", description = "비활성화된 사용자를 활성 상태로 복구합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 활성화 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<org.example.Usersvc.common.response.ApiResponse> activateUser(
            @Parameter(description = "사용자 ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId) {
        
        log.info("Internal API: Activating user {}", userId);
        
        try {
            boolean success = userService.activateUser(userId);
            
            if (success) {
                log.info("Successfully activated user: {}", userId);
                return ResponseEntity.ok(
                    org.example.Usersvc.common.response.ApiResponse.success("사용자가 성공적으로 활성화되었습니다.", "ACTIVE")
                );
            } else {
                log.warn("Failed to activate user: {} - User not found", userId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Error activating user: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(org.example.Usersvc.common.response.ApiResponse.error("사용자 활성화 중 오류가 발생했습니다.", e.getMessage()));
        }
    }

    /**
     * 사용자 상태 조회 (내부용)
     * 
     * 특정 사용자의 현재 상태를 조회합니다.
     * 내부 시스템에서 사용자 상태 확인 시 사용됩니다.
     * 
     * @param userId 조회할 사용자 ID
     * @return 사용자 상태 정보
     */
    @GetMapping("/{userId}/status")
    @Operation(summary = "사용자 상태 조회", description = "사용자의 현재 상태를 조회합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상태 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<org.example.Usersvc.common.response.ApiResponse<String>> getUserStatus(
            @Parameter(description = "사용자 ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId) {
        
        log.debug("Internal API: Getting user status for {}", userId);
        
        try {
            String status = userService.getUserStatus(userId);
            
            if (status != null) {
                return ResponseEntity.ok(
                    org.example.Usersvc.common.response.ApiResponse.success("사용자 상태 조회 성공", status)
                );
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Error getting user status: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(org.example.Usersvc.common.response.ApiResponse.error("사용자 상태 조회 중 오류가 발생했습니다.", e.getMessage()));
        }
    }
}