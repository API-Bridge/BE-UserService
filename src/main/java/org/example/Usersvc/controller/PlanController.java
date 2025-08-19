package org.example.Usersvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.service.ApiUsageTrackingService;
import org.example.Usersvc.service.PlanLimitValidationService;
import org.example.Usersvc.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 플랜 제한 관리 컨트롤러
 * 
 * 사용자의 플랜별 제한사항 조회 및 사용량 통계를 제공합니다.
 */
@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Plan Management", description = "플랜 제한 및 사용량 관리 API")
public class PlanController {

    private final PlanLimitValidationService planLimitValidationService;
    private final ApiUsageTrackingService apiUsageTrackingService;
    private final UserService userService;

    /**
     * 현재 사용자의 플랜 제한 조회
     */
    @GetMapping("/limits")
    @Operation(summary = "플랜 제한 조회", description = "사용자의 현재 플랜별 제한사항 및 사용량 조회")
    public ResponseEntity<ApiResponse<PlanLimitValidationService.PlanLimitsInfo>> getPlanLimits(
            @Parameter(description = "사용자 ID", required = true) 
            @RequestHeader("X-User-Id") String userId) {
        
        try {
            log.info("플랜 제한 조회 요청 - userId: {}", userId);
            
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            
            PlanLimitValidationService.PlanLimitsInfo planLimits = 
                    planLimitValidationService.getPlanLimits(user);
            
            return ResponseEntity.ok(ApiResponse.success(planLimits));
            
        } catch (Exception e) {
            log.error("플랜 제한 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("플랜 제한 조회 실패: " + e.getMessage(), "PLAN_LIMITS_RETRIEVAL_FAILED"));
        }
    }

    /**
     * 사용량 통계 조회
     */
    @GetMapping("/usage-stats")
    @Operation(summary = "사용량 통계 조회", description = "분/시간/일/월별 API 사용량 통계 조회")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsageStats(
            @Parameter(description = "사용자 ID", required = true) 
            @RequestHeader("X-User-Id") String userId) {
        
        try {
            log.info("사용량 통계 조회 요청 - userId: {}", userId);
            
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            
            // 각종 사용량 통계 수집
            long minuteUsage = apiUsageTrackingService.getCurrentMinuteUsage(user);
            long hourUsage = apiUsageTrackingService.getCurrentHourUsage(user);
            long dayUsage = apiUsageTrackingService.getCurrentDayUsage(user);
            long monthlyUsage = apiUsageTrackingService.getMonthlyUsage(user);
            
            Map<String, Object> usageStats = Map.of(
                "currentMinute", minuteUsage,
                "currentHour", hourUsage, 
                "currentDay", dayUsage,
                "currentMonth", monthlyUsage,
                "userId", userId,
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(ApiResponse.success(usageStats));
            
        } catch (Exception e) {
            log.error("사용량 통계 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("사용량 통계 조회 실패: " + e.getMessage(), "USAGE_STATS_RETRIEVAL_FAILED"));
        }
    }

    /**
     * 플랜별 기능 조회
     */
    @GetMapping("/features")
    @Operation(summary = "플랜 기능 조회", description = "사용자의 현재 플랜에서 사용 가능한 기능 목록 조회")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPlanFeatures(
            @Parameter(description = "사용자 ID", required = true) 
            @RequestHeader("X-User-Id") String userId) {
        
        try {
            log.info("플랜 기능 조회 요청 - userId: {}", userId);
            
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            
            PlanLimitValidationService.PlanLimitsInfo planLimits = 
                    planLimitValidationService.getPlanLimits(user);
            
            boolean canCreateCustomApi = planLimitValidationService.canCreateCustomApi(user);
            boolean canShareApi = planLimitValidationService.canShareApi(user);
            
            Map<String, Object> features = Map.of(
                "planType", planLimits.getPlanType().name(),
                "canCreateCustomApi", canCreateCustomApi,
                "canShareApi", canShareApi,
                "customApiRemaining", planLimits.getCustomApiRemaining(),
                "sharedApiRemaining", planLimits.getSharedApiRemaining(),
                "rateLimits", Map.of(
                    "perMinute", planLimits.getRateLimitPerMinute(),
                    "perHour", planLimits.getRateLimitPerHour(),
                    "perDay", planLimits.getRateLimitPerDay()
                ),
                "maxApiCallsPerMonth", planLimits.getMaxApiCallsPerMonth(),
                "maxDataBundleCount", planLimits.getMaxDataBundleCount()
            );
            
            return ResponseEntity.ok(ApiResponse.success(features));
            
        } catch (Exception e) {
            log.error("플랜 기능 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("플랜 기능 조회 실패: " + e.getMessage(), "PLAN_FEATURES_RETRIEVAL_FAILED"));
        }
    }

    /**
     * 구독 제한 상태 조회
     */
    @GetMapping("/limits-status")
    @Operation(summary = "구독 제한 상태 조회", description = "현재 구독 플랜의 제한 상태 및 사용량 확인")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLimitsStatus(
            @Parameter(description = "사용자 ID", required = true) 
            @RequestHeader("X-User-Id") String userId) {
        
        try {
            log.info("구독 제한 상태 조회 요청 - userId: {}", userId);
            
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            
            PlanLimitValidationService.PlanLimitsInfo planLimits = 
                    planLimitValidationService.getPlanLimits(user);
                    
            // 현재 사용량과 제한 비교
            long currentDayUsage = apiUsageTrackingService.getCurrentDayUsage(user);
            long currentMonthlyUsage = apiUsageTrackingService.getMonthlyUsage(user);
            
            boolean isCustomApiLimitReached = planLimits.isCustomApiLimitReached();
            boolean isSharedApiLimitReached = planLimits.isSharedApiLimitReached();
            
            // 플랜 업그레이드 필요 여부 확인
            boolean needsUpgradeForCustomApi = planLimitValidationService.needsPlanUpgrade(user, "CUSTOM_API");
            boolean needsUpgradeForSharedApi = planLimitValidationService.needsPlanUpgrade(user, "SHARED_API");
            
            Map<String, Object> status = Map.of(
                "planType", planLimits.getPlanType().name(),
                "limits", Map.of(
                    "customApi", Map.of(
                        "current", planLimits.getCurrentCustomApiCount(),
                        "max", planLimits.getMaxCustomApiCount(),
                        "remaining", planLimits.getCustomApiRemaining(),
                        "limitReached", isCustomApiLimitReached,
                        "needsUpgrade", needsUpgradeForCustomApi
                    ),
                    "sharedApi", Map.of(
                        "current", planLimits.getCurrentSharedApiCount(),
                        "max", planLimits.getMaxSharedApiCount(),
                        "remaining", planLimits.getSharedApiRemaining(),
                        "limitReached", isSharedApiLimitReached,
                        "needsUpgrade", needsUpgradeForSharedApi
                    ),
                    "usage", Map.of(
                        "dailyApiCalls", currentDayUsage,
                        "monthlyApiCalls", currentMonthlyUsage,
                        "maxMonthlyApiCalls", planLimits.getMaxApiCallsPerMonth()
                    )
                ),
                "recommendations", Map.of(
                    "upgradeRecommended", needsUpgradeForCustomApi || needsUpgradeForSharedApi,
                    "reason", needsUpgradeForCustomApi ? "Custom API 생성 한도 초과" : 
                              needsUpgradeForSharedApi ? "공유 API 한도 초과" : null
                )
            );
            
            return ResponseEntity.ok(ApiResponse.success(status));
            
        } catch (Exception e) {
            log.error("구독 제한 상태 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("구독 제한 상태 조회 실패: " + e.getMessage(), "LIMITS_STATUS_RETRIEVAL_FAILED"));
        }
    }
}