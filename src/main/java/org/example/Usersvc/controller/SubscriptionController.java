package org.example.Usersvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.domain.SubscriptionPlan;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.service.DevRateLimitService;
import org.example.Usersvc.service.RateLimitService;
import org.example.Usersvc.service.UserService;
import org.example.Usersvc.service.UserSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 구독 및 API 사용량 관리 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Subscription & Usage", description = "구독 관리 및 API 사용량 조회 API")
public class SubscriptionController {
    
    private final UserService userService;
    private final UserSubscriptionService subscriptionService;
    
    @Autowired(required = false)
    private RateLimitService rateLimitService;
    
    @Autowired(required = false) 
    private DevRateLimitService devRateLimitService;
    
    @Operation(
            summary = "사용자 구독 정보 조회",
            description = "사용자의 현재 활성 구독 정보를 조회합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "구독 정보 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/users/{userId}/subscription")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<UserSubscription>> getUserSubscription(
            @Parameter(description = "사용자 ID", example = "1") @PathVariable Long userId) {
        
        try {
            User user = userService.findByUserId(userId);
            UserSubscription subscription = subscriptionService.getActiveSubscription(user);
            
            return ResponseEntity.ok(ApiResponse.success(subscription));
            
        } catch (Exception e) {
            log.error("구독 정보 조회 중 오류 발생 - userId: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("구독 정보 조회에 실패했습니다.", "SUBSCRIPTION_ERROR"));
        }
    }
    
    @Operation(
            summary = "구독 플랜 변경",
            description = "사용자의 구독 플랜을 변경합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/users/{userId}/subscription")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<UserSubscription>> changePlan(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Parameter(description = "새로운 플랜명") @RequestParam String planName) {
        
        try {
            User user = userService.findByUserId(userId);
            UserSubscription newSubscription = subscriptionService.changePlan(user, planName);
            
            return ResponseEntity.ok(ApiResponse.success(newSubscription));
            
        } catch (Exception e) {
            log.error("플랜 변경 중 오류 발생 - userId: {}, planName: {}", userId, planName, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("플랜 변경에 실패했습니다.", "PLAN_CHANGE_ERROR"));
        }
    }
    
    @Operation(
            summary = "API 사용량 조회",
            description = "사용자의 현재 API 사용량을 조회합니다. (분당, 시간당, 일일)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/users/{userId}/usage")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getApiUsage(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        
        try {
            User user = userService.findByUserId(userId);
            UserSubscription subscription = subscriptionService.getActiveSubscription(user);
            SubscriptionPlan plan = subscription.getPlan();
            
            Map<String, Object> usageInfo = new HashMap<>();
            
            // 현재 사용량 조회
            long minuteUsage, hourUsage, dayUsage;
            
            if (devRateLimitService != null) {
                // 개발 환경
                minuteUsage = devRateLimitService.getCurrentMinuteUsage(user);
                hourUsage = devRateLimitService.getCurrentHourUsage(user);
                dayUsage = devRateLimitService.getCurrentDayUsage(user);
            } else {
                // 프로덕션 환경
                minuteUsage = rateLimitService.getCurrentMinuteUsage(user);
                hourUsage = rateLimitService.getCurrentHourUsage(user);
                dayUsage = rateLimitService.getCurrentDayUsage(user);
            }
            
            // 플랜 정보
            usageInfo.put("plan", Map.of(
                    "name", plan.getPlanName(),
                    "maxApiCount", plan.getMaxApiCount(),
                    "rateLimitPerMinute", plan.getRateLimitPerMinute(),
                    "rateLimitPerHour", plan.getRateLimitPerHour(),
                    "rateLimitPerDay", plan.getRateLimitPerDay()
            ));
            
            // 현재 사용량
            usageInfo.put("currentUsage", Map.of(
                    "minute", Map.of("used", minuteUsage, "limit", plan.getRateLimitPerMinute()),
                    "hour", Map.of("used", hourUsage, "limit", plan.getRateLimitPerHour()),
                    "day", Map.of("used", dayUsage, "limit", plan.getRateLimitPerDay())
            ));
            
            // 사용률 계산
            usageInfo.put("usageRatio", Map.of(
                    "minute", (double) minuteUsage / plan.getRateLimitPerMinute() * 100,
                    "hour", (double) hourUsage / plan.getRateLimitPerHour() * 100,
                    "day", (double) dayUsage / plan.getRateLimitPerDay() * 100
            ));
            
            return ResponseEntity.ok(ApiResponse.success(usageInfo));
            
        } catch (Exception e) {
            log.error("사용량 조회 중 오류 발생 - userId: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("사용량 조회에 실패했습니다.", "USAGE_ERROR"));
        }
    }
}