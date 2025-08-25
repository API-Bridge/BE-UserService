package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.PlanName;
import org.example.Usersvc.domain.User;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 프로덕션 환경용 Rate Limiting 서비스
 * 
 * 데이터베이스 기반으로 사용자의 API 호출 제한을 관리합니다.
 * - 플랜별 제한 적용
 * - 분/시간/일별 제한 검증
 * - 실제 사용량 기반 제한
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"prod", "production"})
public class ProductionRateLimitService {

    private final ApiUsageTrackingService apiUsageTrackingService;
    private final PlanLimitValidationService planLimitValidationService;

    /**
     * 분당 호출 제한 확인
     * 
     * @param user 사용자
     * @return 허용 여부
     */
    public boolean isAllowedPerMinute(User user) {
        try {
            PlanName planName = getPlanName(user);
            long currentUsage = apiUsageTrackingService.getCurrentMinuteUsage(user);
            int limit = planName.getRateLimitPerMinute();
            
            boolean allowed = currentUsage < limit;
            
            log.debug("분당 제한 확인 - userId: {}, plan: {}, usage: {}/{}, allowed: {}", 
                    user.getUserId(), planName, currentUsage, limit, allowed);
                    
            return allowed;
        } catch (Exception e) {
            log.error("분당 제한 확인 실패 - userId: {}", user.getUserId(), e);
            // 에러 시 허용 (서비스 중단 방지)
            return true;
        }
    }

    /**
     * 시간당 호출 제한 확인
     * 
     * @param user 사용자
     * @return 허용 여부
     */
    public boolean isAllowedPerHour(User user) {
        try {
            PlanName planName = getPlanName(user);
            long currentUsage = apiUsageTrackingService.getCurrentHourUsage(user);
            int limit = planName.getRateLimitPerHour();
            
            boolean allowed = currentUsage < limit;
            
            log.debug("시간당 제한 확인 - userId: {}, plan: {}, usage: {}/{}, allowed: {}", 
                    user.getUserId(), planName, currentUsage, limit, allowed);
                    
            return allowed;
        } catch (Exception e) {
            log.error("시간당 제한 확인 실패 - userId: {}", user.getUserId(), e);
            return true;
        }
    }

    /**
     * 일일 호출 제한 확인
     * 
     * @param user 사용자
     * @return 허용 여부
     */
    public boolean isAllowedPerDay(User user) {
        try {
            PlanName planName = getPlanName(user);
            long currentUsage = apiUsageTrackingService.getCurrentDayUsage(user);
            int limit = planName.getRateLimitPerDay();
            
            boolean allowed = currentUsage < limit;
            
            log.debug("일일 제한 확인 - userId: {}, plan: {}, usage: {}/{}, allowed: {}", 
                    user.getUserId(), planName, currentUsage, limit, allowed);
                    
            return allowed;
        } catch (Exception e) {
            log.error("일일 제한 확인 실패 - userId: {}", user.getUserId(), e);
            return true;
        }
    }

    /**
     * 월별 호출 제한 확인
     * 
     * @param user 사용자
     * @return 허용 여부
     */
    public boolean isAllowedPerMonth(User user) {
        try {
            PlanName planName = getPlanName(user);
            long currentUsage = apiUsageTrackingService.getMonthlyUsage(user);
            int limit = planName.getMaxApiCount();
            
            boolean allowed = currentUsage < limit;
            
            log.debug("월별 제한 확인 - userId: {}, plan: {}, usage: {}/{}, allowed: {}", 
                    user.getUserId(), planName, currentUsage, limit, allowed);
                    
            return allowed;
        } catch (Exception e) {
            log.error("월별 제한 확인 실패 - userId: {}", user.getUserId(), e);
            return true;
        }
    }

    /**
     * 전체 Rate Limit 확인
     * 모든 제한(분/시간/일/월)을 한번에 확인합니다.
     * 
     * @param user 사용자
     * @return 허용 여부
     */
    public boolean isAllowed(User user) {
        return isAllowedPerMinute(user) && 
               isAllowedPerHour(user) && 
               isAllowedPerDay(user) && 
               isAllowedPerMonth(user);
    }

    /**
     * Rate Limit 상태 조회
     * 
     * @param user 사용자
     * @return Rate Limit 상태 정보
     */
    public RateLimitStatus getRateLimitStatus(User user) {
        try {
            PlanName planName = getPlanName(user);
            
            long minuteUsage = apiUsageTrackingService.getCurrentMinuteUsage(user);
            long hourUsage = apiUsageTrackingService.getCurrentHourUsage(user);
            long dayUsage = apiUsageTrackingService.getCurrentDayUsage(user);
            long monthUsage = apiUsageTrackingService.getMonthlyUsage(user);
            
            return RateLimitStatus.builder()
                    .userId(user.getUserId())
                    .planName(planName)
                    .minuteUsage(minuteUsage)
                    .minuteLimit(planName.getRateLimitPerMinute())
                    .hourUsage(hourUsage)
                    .hourLimit(planName.getRateLimitPerHour())
                    .dayUsage(dayUsage)
                    .dayLimit(planName.getRateLimitPerDay())
                    .monthUsage(monthUsage)
                    .monthLimit(planName.getMaxApiCount())
                    .isAllowed(isAllowed(user))
                    .checkedAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Rate limit 상태 조회 실패 - userId: {}", user.getUserId(), e);
            
            // 에러 시 기본값 반환
            return RateLimitStatus.builder()
                    .userId(user.getUserId())
                    .planName(PlanName.FREE)
                    .isAllowed(true)
                    .checkedAt(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * 제한 초과까지 남은 횟수 계산
     * 
     * @param user 사용자
     * @return 남은 허용 횟수
     */
    public RemainingLimits getRemainingLimits(User user) {
        try {
            PlanName planName = getPlanName(user);
            
            long minuteUsage = apiUsageTrackingService.getCurrentMinuteUsage(user);
            long hourUsage = apiUsageTrackingService.getCurrentHourUsage(user);
            long dayUsage = apiUsageTrackingService.getCurrentDayUsage(user);
            long monthUsage = apiUsageTrackingService.getMonthlyUsage(user);
            
            return RemainingLimits.builder()
                    .minuteRemaining(Math.max(0, planName.getRateLimitPerMinute() - minuteUsage))
                    .hourRemaining(Math.max(0, planName.getRateLimitPerHour() - hourUsage))
                    .dayRemaining(Math.max(0, planName.getRateLimitPerDay() - dayUsage))
                    .monthRemaining(Math.max(0, planName.getMaxApiCount() - monthUsage))
                    .build();
                    
        } catch (Exception e) {
            log.error("남은 제한 횟수 계산 실패 - userId: {}", user.getUserId(), e);
            return RemainingLimits.builder().build();
        }
    }

    /**
     * 사용자의 현재 플랜 타입 조회
     */
    private PlanName getPlanName(User user) {
        return planLimitValidationService.getPlanLimits(user).getPlanName();
    }

    /**
     * Rate Limit 상태 정보 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class RateLimitStatus {
        private final String userId;
        private final PlanName planName;
        private final long minuteUsage;
        private final int minuteLimit;
        private final long hourUsage;
        private final int hourLimit;
        private final long dayUsage;
        private final int dayLimit;
        private final long monthUsage;
        private final int monthLimit;
        private final boolean isAllowed;
        private final LocalDateTime checkedAt;
        
        public boolean isMinuteLimitExceeded() {
            return minuteUsage >= minuteLimit;
        }
        
        public boolean isHourLimitExceeded() {
            return hourUsage >= hourLimit;
        }
        
        public boolean isDayLimitExceeded() {
            return dayUsage >= dayLimit;
        }
        
        public boolean isMonthLimitExceeded() {
            return monthUsage >= monthLimit;
        }
    }

    /**
     * 남은 제한 횟수 정보 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class RemainingLimits {
        @lombok.Builder.Default
        private final long minuteRemaining = 0;
        @lombok.Builder.Default
        private final long hourRemaining = 0;
        @lombok.Builder.Default
        private final long dayRemaining = 0;
        @lombok.Builder.Default
        private final long monthRemaining = 0;
        
        public long getMinimumRemaining() {
            return Math.min(Math.min(minuteRemaining, hourRemaining), 
                           Math.min(dayRemaining, monthRemaining));
        }
    }
}