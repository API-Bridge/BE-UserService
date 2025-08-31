package org.example.Usersvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.common.constants.ErrorConstants;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.service.UserService;
import org.example.Usersvc.service.AdminService;
import org.example.Usersvc.service.UserSubscriptionMigrationService;
import org.example.Usersvc.service.AdminAuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자 전용 컨트롤러
 * API Gateway에서 사전 검증된 관리자만 접근 가능
 * 
 * 주요 기능:
 * - 전체 사용자 관리
 * - 구독 관리 (플랜 변경, 구독 취소/활성화)
 * - 시스템 통계 조회
 * - 사용량 모니터링
 * - 데이터 마이그레이션
 * 
 * 주의: 이 컨트롤러의 모든 엔드포인트는 API Gateway에서
 *       admin 역할을 사전 검증한 후에만 호출됩니다.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Management", description = "관리자 전용 API - API Gateway에서 사전 검증됨")
public class AdminController {

    private final UserSubscriptionMigrationService migrationService;
    private final UserService userService;
    private final AdminService adminService;
    private final AdminAuthorizationService adminAuthorizationService;

    /**
     * 전체 사용자 목록 조회 (페이징)
     * API Gateway에서 admin 역할이 검증된 후 호출됩니다.
     */
    @Operation(
            summary = "전체 사용자 목록 조회",
            description = "관리자용 전체 사용자 목록을 페이징하여 조회합니다. API Gateway에서 권한이 사전 검증됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "사용자 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                    {
                                        "success": true,
                                        "data": {
                                            "content": [
                                                {
                                                    "userId": "user-001",
                                                    "auth0Id": "auth0|testuser001",
                                                    "userEmail": "test@example.com",
                                                    "createdAt": "2024-12-01T10:00:00"
                                                }
                                            ],
                                            "totalElements": 15,
                                            "totalPages": 2,
                                            "number": 0,
                                            "size": 10
                                        }
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<User>>> getAllUsers(
            @Parameter(description = "관리자 사용자 ID", required = false)
            @RequestHeader(value = "X-User-Id", required = false) String adminUserId,
            @Parameter(description = "관리자 Auth0 ID", required = false)
            @RequestParam(value = "adminAuth0Id", required = false) String adminAuth0Id,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("관리자 - 전체 사용자 목록 조회 요청 - adminUserId: {}, adminAuth0Id: {}, page: {}, size: {}", 
                adminUserId, adminAuth0Id, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            // 관리자 권한 검증 (userId 또는 auth0Id로)
            boolean isAdmin = false;
            String actualAdminId = null;
            
            if (adminUserId != null && !adminUserId.trim().isEmpty()) {
                isAdmin = adminAuthorizationService.isActiveAdmin(adminUserId);
                actualAdminId = adminUserId;
            } else if (adminAuth0Id != null && !adminAuth0Id.trim().isEmpty()) {
                isAdmin = adminAuthorizationService.isActiveAdminByAuth0Id(adminAuth0Id);
                actualAdminId = adminAuth0Id;
            }
            
            if (!isAdmin) {
                log.warn("관리자 권한 없음 - 전체 사용자 목록 조회 거부: {}", adminUserId);
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("관리자 권한이 필요합니다.", "ACCESS_DENIED"));
            }
            Page<User> users = userService.getAllUsersWithPagination(pageable);
            
            log.info("관리자({}) - 전체 사용자 목록 조회 성공 - 총 {}명, {}페이지 중 {}페이지", 
                    adminUserId, users.getTotalElements(), users.getTotalPages(), users.getNumber() + 1);
            
            return ResponseEntity.ok(ApiResponse.success(users));
            
        } catch (IllegalArgumentException e) {
            log.warn("관리자({}) - 잘못된 페이징 파라미터: {}", adminUserId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorConstants.INVALID_INPUT_DATA, ErrorConstants.INVALID_REQUEST));
        } catch (Exception e) {
            log.error("관리자({}) - 전체 사용자 목록 조회 중 오류 발생", adminUserId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ErrorConstants.USER_RETRIEVAL_FAILED, ErrorConstants.INTERNAL_ERROR));
        }
    }

    /**
     * 특정 사용자 상세 정보 조회 (구독 정보 포함)
     * API Gateway에서 admin 역할이 검증된 후 호출됩니다.
     */
    @Operation(
            summary = "사용자 상세 정보 조회",
            description = "관리자용 특정 사용자의 상세 정보를 구독 정보와 함께 조회합니다."
    )
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserDetail(
            @Parameter(description = "관리자 사용자 ID", required = true)
            @RequestHeader("X-User-Id") String adminUserId,
            @Parameter(description = "조회할 사용자의 Auth0 ID") @PathVariable String userId) {
        
        log.info("관리자({}) - 사용자 상세 정보 조회 요청 - userId: {}", adminUserId, userId);
        
        try {
            // 관리자 권한 검증
            if (!adminAuthorizationService.isActiveAdmin(adminUserId)) {
                log.warn("관리자 권한 없음 - 사용자 상세 정보 조회 거부: {}", adminUserId);
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("관리자 권한이 필요합니다.", "ACCESS_DENIED"));
            }
            Map<String, Object> userDetail = adminService.getUserCompleteInfo(userId);
            
            log.info("관리자({}) - 사용자 상세 정보 조회 성공 - userId: {}", adminUserId, userId);
            
            return ResponseEntity.ok(ApiResponse.success(userDetail));
            
        } catch (IllegalArgumentException e) {
            log.warn("관리자({}) - 사용자 상세 정보 조회 실패 - userId: {}, error: {}", adminUserId, userId, e.getMessage());
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("관리자({}) - 사용자 상세 정보 조회 중 오류 발생 - userId: {}", adminUserId, userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ErrorConstants.USER_RETRIEVAL_FAILED, ErrorConstants.INTERNAL_ERROR));
        }
    }

    /**
     * 시스템 통계 조회
     * API Gateway에서 admin 역할이 검증된 후 호출됩니다.
     */
    @Operation(
            summary = "시스템 통계 조회",
            description = "관리자용 전체 시스템 통계를 조회합니다."
    )
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStatistics(
            @Parameter(description = "관리자 사용자 ID", required = true)
            @RequestHeader("X-User-Id") String adminUserId) {
        
        log.info("관리자({}) - 시스템 통계 조회 요청", adminUserId);
        
        try {
            // 관리자 권한 검증
            if (!adminAuthorizationService.isActiveAdmin(adminUserId)) {
                log.warn("관리자 권한 없음 - 시스템 통계 조회 거부: {}", adminUserId);
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("관리자 권한이 필요합니다.", "ACCESS_DENIED"));
            }
            Map<String, Object> statistics = adminService.getSystemStatistics();
            
            log.info("관리자({}) - 시스템 통계 조회 성공", adminUserId);
            
            return ResponseEntity.ok(ApiResponse.success(statistics));
            
        } catch (Exception e) {
            log.error("관리자({}) - 시스템 통계 조회 중 오류 발생", adminUserId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ErrorConstants.DATABASE_OPERATION_FAILED, ErrorConstants.INTERNAL_ERROR));
        }
    }

    @PostMapping("/migrate/free-subscriptions")
    @Operation(
        summary = "FREE 구독 자동 생성", 
        description = "구독이 없는 모든 사용자에게 FREE 구독을 자동으로 생성합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> createMissingFreeSubscriptions(
            @Parameter(description = "관리자 사용자 ID", required = true)
            @RequestHeader("X-User-Id") String adminUserId) {
        try {
            // 관리자 권한 검증
            if (!adminAuthorizationService.isActiveAdmin(adminUserId)) {
                log.warn("관리자 권한 없음 - FREE 구독 자동 생성 거부: {}", adminUserId);
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("관리자 권한이 필요합니다.", "ACCESS_DENIED"));
            }
            
            log.info("관리자({}) - FREE 구독 자동 생성 요청 시작", adminUserId);
            
            // 현재 구독이 없는 사용자 수 확인
            long usersWithoutSubscription = migrationService.countUsersWithoutSubscription();
            
            if (usersWithoutSubscription == 0) {
                return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "message", "모든 사용자가 이미 구독을 가지고 있습니다.",
                    "processedCount", 0,
                    "totalUsersWithoutSubscription", 0
                )));
            }
            
            // FREE 구독 자동 생성 실행
            int processedCount = migrationService.createMissingFreeSubscriptions();
            
            Map<String, Object> result = Map.of(
                "message", "FREE 구독 자동 생성이 완료되었습니다.",
                "processedCount", processedCount,
                "totalUsersWithoutSubscription", usersWithoutSubscription
            );
            
            log.info("관리자({}) - FREE 구독 자동 생성 완료 - 처리된 사용자 수: {}", adminUserId, processedCount);
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("관리자({}) - FREE 구독 자동 생성 실패", adminUserId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("FREE 구독 자동 생성에 실패했습니다: " + e.getMessage(), 
                    "MIGRATION_FAILED"));
        }
    }
    
    /**
     * 플랜별 사용자 분포 통계 조회
     * API Gateway에서 admin 역할이 검증된 후 호출됩니다.
     */
    @Operation(
            summary = "플랜별 사용자 분포 통계 조회",
            description = "관리자용 플랜별 구독자 분포 및 통계를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "플랜 분포 통계 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                    {
                                        "success": true,
                                        "data": {
                                            "planDistribution": {
                                                "FREE": 150,
                                                "BASIC": 45,
                                                "PREMIUM": 23,
                                                "ENTERPRISE": 5
                                            },
                                            "totalActivePlans": 223
                                        }
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/statistics/plan-distribution")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPlanDistributionStatistics(
            @Parameter(description = "관리자 사용자 ID", required = true)
            @RequestHeader("X-User-Id") String adminUserId) {
        
        log.info("관리자({}) - 플랜별 분포 통계 조회 요청", adminUserId);
        
        try {
            // 관리자 권한 검증
            if (!adminAuthorizationService.isActiveAdmin(adminUserId)) {
                log.warn("관리자 권한 없음 - 플랜별 분포 통계 조회 거부: {}", adminUserId);
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("관리자 권한이 필요합니다.", "ACCESS_DENIED"));
            }
            
            Map<String, Object> planDistribution = adminService.getPlanDistributionStatistics();
            
            log.info("관리자({}) - 플랜별 분포 통계 조회 성공 - 총 활성 구독: {}", 
                    adminUserId, planDistribution.get("totalActivePlans"));
            
            return ResponseEntity.ok(ApiResponse.success(planDistribution));
            
        } catch (Exception e) {
            log.error("관리자({}) - 플랜별 분포 통계 조회 중 오류 발생", adminUserId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ErrorConstants.DATABASE_OPERATION_FAILED, ErrorConstants.INTERNAL_ERROR));
        }
    }
    
    @GetMapping("/users/without-subscription/count")
    @Operation(
        summary = "구독이 없는 사용자 수 조회", 
        description = "현재 구독이 없는 사용자의 수를 조회합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsersWithoutSubscriptionCount(
            @Parameter(description = "관리자 사용자 ID", required = true)
            @RequestHeader("X-User-Id") String adminUserId) {
        try {
            // 관리자 권한 검증
            if (!adminAuthorizationService.isActiveAdmin(adminUserId)) {
                log.warn("관리자 권한 없음 - 구독 없는 사용자 수 조회 거부: {}", adminUserId);
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("관리자 권한이 필요합니다.", "ACCESS_DENIED"));
            }
            
            long count = migrationService.countUsersWithoutSubscription();
            
            Map<String, Object> result = Map.of(
                "usersWithoutSubscription", count,
                "message", count > 0 ? 
                    count + "명의 사용자가 구독이 없습니다." : 
                    "모든 사용자가 구독을 가지고 있습니다."
            );
            
            log.info("관리자({}) - 구독 없는 사용자 수 조회: {}명", adminUserId, count);
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("관리자({}) - 구독이 없는 사용자 수 조회 실패", adminUserId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("구독이 없는 사용자 수 조회에 실패했습니다: " + e.getMessage(), 
                    "COUNT_FAILED"));
        }
    }
}