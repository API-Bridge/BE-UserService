package org.example.Usersvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.service.ApiUsageTrackingService;
import org.example.Usersvc.service.UserService;
import org.example.Usersvc.util.HeaderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 플랜 관리 컨트롤러 (사용량 통계만 제공)
 */
@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Plan Management", description = "플랜 사용량 통계 API")
public class PlanController {

    @Autowired(required = false)
    private ApiUsageTrackingService apiUsageTrackingService;
    
    private final UserService userService;


    /**
     * 사용량 통계 조회
     */
    @GetMapping("/usage-stats")
    @Operation(summary = "사용량 통계 조회", description = "분/시간/일/월별 API 사용량 통계 조회")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsageStats(
            @Parameter(description = "사용자 ID", required = true) 
            @RequestHeader("X-User-Id") String userId) {
        
        try {
            // X-User-Id 헤더 정리 (쉼표로 구분된 경우 첫 번째 값 사용)
            String actualUserId = HeaderUtils.extractUserId(userId);
            if (actualUserId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("유효하지 않은 사용자 ID", "INVALID_USER_ID"));
            }
            
            log.info("사용량 통계 조회 요청 - userId: {}", actualUserId);
            
            User user = userService.getUserById(actualUserId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + actualUserId));
            
            // 각종 사용량 통계 수집
            long minuteUsage = 0;
            long hourUsage = 0;
            long dayUsage = 0;
            long monthlyUsage = 0;
            
            if (apiUsageTrackingService != null) {
                minuteUsage = apiUsageTrackingService.getCurrentMinuteUsage(user);
                hourUsage = apiUsageTrackingService.getCurrentHourUsage(user);
                dayUsage = apiUsageTrackingService.getCurrentDayUsage(user);
                monthlyUsage = apiUsageTrackingService.getMonthlyUsage(user);
            } else {
                log.warn("ApiUsageTrackingService가 주입되지 않았습니다. 기본값 0을 사용합니다.");
            }
            
            Map<String, Object> usageStats = Map.of(
                "currentMinute", minuteUsage,
                "currentHour", hourUsage, 
                "currentDay", dayUsage,
                "currentMonth", monthlyUsage,
                "userId", actualUserId,
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(ApiResponse.success(usageStats));
            
        } catch (Exception e) {
            log.error("사용량 통계 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("사용량 통계 조회 실패: " + e.getMessage(), "USAGE_STATS_RETRIEVAL_FAILED"));
        }
    }


}