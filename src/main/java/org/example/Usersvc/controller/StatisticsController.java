package org.example.Usersvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.service.ActiveUserTracker;
import org.example.Usersvc.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Map;

/**
 * 활성 사용자 통계 컨트롤러
 * DAU, WAU, MAU 측정 결과 제공
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Statistics", description = "활성 사용자 통계 API (DAU/WAU/MAU) - 관리자 전용")
public class StatisticsController {

    private final ActiveUserTracker activeUserTracker;
    private final UserService userService;

    /**
     * 관리자 권한 체크
     * X-User-Id 헤더에서 사용자를 추출하고 관리자 권한을 확인합니다.
     */
    private ResponseEntity<ApiResponse<Object>> checkAdminAccess(HttpServletRequest request) {
        try {
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
                log.warn("통계 API 접근 시도 - X-User-Id 헤더 없음");
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증이 필요합니다.", "AUTHENTICATION_REQUIRED"));
            }

            User user = userService.getUserById(userIdHeader.trim())
                .orElse(null);
            
            if (user == null) {
                log.warn("통계 API 접근 시도 - 존재하지 않는 사용자: {}", userIdHeader);
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("유효하지 않은 사용자입니다.", "INVALID_USER"));
            }

            if (!user.isAdmin()) {
                log.warn("통계 API 접근 거부 - 관리자 권한 없음, userId: {}", user.getUserId());
                return ResponseEntity.status(403)
                    .body(ApiResponse.error("관리자 권한이 필요합니다.", "ACCESS_DENIED"));
            }

            log.info("관리자 통계 API 접근 승인 - userId: {}", user.getUserId());
            return null; // null이면 권한 확인 통과
            
        } catch (Exception e) {
            log.error("관리자 권한 확인 중 오류 발생", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("권한 확인 중 오류가 발생했습니다.", "PERMISSION_CHECK_FAILED"));
        }
    }

    /**
     * DAU (Daily Active Users) 조회
     */
    @GetMapping("/dau")
    @Operation(summary = "DAU 조회", description = "특정 날짜의 일간 활성 사용자 수 조회 (관리자 전용)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDailyActiveUsers(
            HttpServletRequest request,
            @Parameter(description = "조회 날짜 (YYYY-MM-DD 형식, 생략 시 오늘)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        // 관리자 권한 체크
        ResponseEntity<ApiResponse<Object>> adminCheck = checkAdminAccess(request);
        if (adminCheck != null) {
            return ResponseEntity.status(adminCheck.getStatusCode())
                    .body(ApiResponse.error(adminCheck.getBody().getMessage(), adminCheck.getBody().getErrorCode()));
        }
        
        try {
            LocalDate targetDate = date != null ? date : LocalDate.now();
            
            log.info("DAU 조회 요청 - date: {}", targetDate);
            
            long dauCount = activeUserTracker.getDailyActiveUsers(targetDate);
            
            Map<String, Object> result = Map.of(
                "dau", dauCount,
                "date", targetDate,
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("DAU 조회 실패 - date: {}, error: {}", date, e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("DAU 조회 실패: " + e.getMessage(), "DAU_RETRIEVAL_FAILED"));
        }
    }

    /**
     * WAU (Weekly Active Users) 조회
     */
    @GetMapping("/wau")
    @Operation(summary = "WAU 조회", description = "현재 주의 주간 활성 사용자 수 조회 (관리자 전용)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWeeklyActiveUsers(HttpServletRequest request) {
        
        // 관리자 권한 체크
        ResponseEntity<ApiResponse<Object>> adminCheck = checkAdminAccess(request);
        if (adminCheck != null) {
            assert adminCheck.getBody() != null;
            return ResponseEntity.status(adminCheck.getStatusCode())
                    .body(ApiResponse.error(adminCheck.getBody().getMessage(), adminCheck.getBody().getErrorCode()));
        }
        
        try {
            log.info("WAU 조회 요청");
            
            long wauCount = activeUserTracker.getWeeklyActiveUsers();
            LocalDate today = LocalDate.now();
            
            Map<String, Object> result = Map.of(
                "wau", wauCount,
                "weekOf", today,
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("WAU 조회 실패 - error: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("WAU 조회 실패: " + e.getMessage(), "WAU_RETRIEVAL_FAILED"));
        }
    }

    /**
     * MAU (Monthly Active Users) 조회
     */
    @GetMapping("/mau")
    @Operation(summary = "MAU 조회", description = "현재 월의 월간 활성 사용자 수 조회 (관리자 전용)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlyActiveUsers(HttpServletRequest request) {
        
        // 관리자 권한 체크
        ResponseEntity<ApiResponse<Object>> adminCheck = checkAdminAccess(request);
        if (adminCheck != null) {
            return ResponseEntity.status(adminCheck.getStatusCode())
                    .body(ApiResponse.error(adminCheck.getBody().getMessage(), adminCheck.getBody().getErrorCode()));
        }
        
        try {
            log.info("MAU 조회 요청");
            
            long mauCount = activeUserTracker.getMonthlyActiveUsers();
            LocalDate today = LocalDate.now();
            
            Map<String, Object> result = Map.of(
                "mau", mauCount,
                "monthOf", today.withDayOfMonth(1), // 월 시작일
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("MAU 조회 실패 - error: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("MAU 조회 실패: " + e.getMessage(), "MAU_RETRIEVAL_FAILED"));
        }
    }

    /**
     * 통합 통계 조회 (DAU, WAU, MAU 모두)
     */
    @GetMapping("/summary")
    @Operation(summary = "통합 통계 조회", description = "DAU, WAU, MAU를 한 번에 조회 (관리자 전용)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatsSummary(HttpServletRequest request) {
        
        // 관리자 권한 체크
        ResponseEntity<ApiResponse<Object>> adminCheck = checkAdminAccess(request);
        if (adminCheck != null) {
            return ResponseEntity.status(adminCheck.getStatusCode())
                    .body(ApiResponse.error(adminCheck.getBody().getMessage(), adminCheck.getBody().getErrorCode()));
        }
        
        try {
            log.info("통합 통계 조회 요청");
            
            LocalDate today = LocalDate.now();
            long dauCount = activeUserTracker.getDailyActiveUsers(today);
            long wauCount = activeUserTracker.getWeeklyActiveUsers();
            long mauCount = activeUserTracker.getMonthlyActiveUsers();
            
            Map<String, Object> result = Map.of(
                "dau", dauCount,
                "wau", wauCount,
                "mau", mauCount,
                "date", today,
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("통합 통계 조회 실패 - error: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("통합 통계 조회 실패: " + e.getMessage(), "STATS_SUMMARY_FAILED"));
        }
    }

    /**
     * 개발/테스트용 카운터 초기화
     */
    @DeleteMapping("/reset")
    @Operation(summary = "카운터 초기화", description = "개발/테스트용 모든 활성 사용자 카운터 초기화 (관리자 전용)")
    public ResponseEntity<ApiResponse<String>> resetCounters(HttpServletRequest request) {
        
        // 관리자 권한 체크
        ResponseEntity<ApiResponse<Object>> adminCheck = checkAdminAccess(request);
        if (adminCheck != null) {
            return ResponseEntity.status(adminCheck.getStatusCode())
                    .body(ApiResponse.error(adminCheck.getBody().getMessage(), adminCheck.getBody().getErrorCode()));
        }
        
        try {
            log.info("활성 사용자 카운터 초기화 요청");
            
            activeUserTracker.resetAllCounters();
            
            return ResponseEntity.ok(ApiResponse.success("모든 활성 사용자 카운터가 초기화되었습니다."));
            
        } catch (Exception e) {
            log.error("카운터 초기화 실패 - error: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("카운터 초기화 실패: " + e.getMessage(), "COUNTER_RESET_FAILED"));
        }
    }

}