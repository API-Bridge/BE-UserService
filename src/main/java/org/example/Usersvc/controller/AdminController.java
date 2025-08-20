package org.example.Usersvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.service.UserSubscriptionMigrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 관리자 전용 컨트롤러
 * 
 * 데이터 마이그레이션, 시스템 관리 등의 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "관리자 전용 API")
public class AdminController {

    private final UserSubscriptionMigrationService migrationService;

    @PostMapping("/migrate/free-subscriptions")
    @Operation(
        summary = "FREE 구독 자동 생성", 
        description = "구독이 없는 모든 사용자에게 FREE 구독을 자동으로 생성합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> createMissingFreeSubscriptions() {
        try {
            log.info("FREE 구독 자동 생성 요청 시작");
            
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
            
            log.info("FREE 구독 자동 생성 완료 - 처리된 사용자 수: {}", processedCount);
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("FREE 구독 자동 생성 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("FREE 구독 자동 생성에 실패했습니다: " + e.getMessage(), 
                    "MIGRATION_FAILED"));
        }
    }
    
    @GetMapping("/users/without-subscription/count")
    @Operation(
        summary = "구독이 없는 사용자 수 조회", 
        description = "현재 구독이 없는 사용자의 수를 조회합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsersWithoutSubscriptionCount() {
        try {
            long count = migrationService.countUsersWithoutSubscription();
            
            Map<String, Object> result = Map.of(
                "usersWithoutSubscription", count,
                "message", count > 0 ? 
                    count + "명의 사용자가 구독이 없습니다." : 
                    "모든 사용자가 구독을 가지고 있습니다."
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("구독이 없는 사용자 수 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("구독이 없는 사용자 수 조회에 실패했습니다: " + e.getMessage(), 
                    "COUNT_FAILED"));
        }
    }
}
