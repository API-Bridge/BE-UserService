package org.example.Usersvc.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.SubscriptionPlan;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.repository.SubscriptionPlanRepository;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SubscriptionPlanManagementService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public List<SubscriptionPlan> getAllActivePlans() {
        return subscriptionPlanRepository.findAllByOrderByPrice();
    }

    public Optional<SubscriptionPlan> getPlanByName(String planName) {
        return subscriptionPlanRepository.findByPlanName(planName);
    }

    public SubscriptionPlan getFreePlan() {
        return subscriptionPlanRepository.findFreePlan()
                .orElseThrow(() -> new IllegalStateException("Free 플랜을 찾을 수 없습니다."));
    }

    public boolean isApiCreationLimitExceeded(SubscriptionPlan plan, int currentApiCount) {
        return currentApiCount > plan.getMaxApiCount();
    }

    public RateLimitInfo getRateLimitInfo(SubscriptionPlan plan) {
        return new RateLimitInfo(
                plan.getRateLimitPerMinute(),
                plan.getRateLimitPerHour(),
                plan.getRateLimitPerDay()
        );
    }

    public boolean isUpgrade(SubscriptionPlan currentPlan, SubscriptionPlan targetPlan) {
        return targetPlan.getMonthlyPrice().compareTo(currentPlan.getMonthlyPrice()) > 0;
    }

    public PlanFeatures getPlanFeatures(SubscriptionPlan plan) {
        PlanFeatures features = new PlanFeatures();
        features.setMaxApiCount(plan.getMaxApiCount());
        features.setApiSharingAllowed(true);

        switch (plan.getPlanName().toUpperCase()) {
            case "FREE":
                features.setMaxDataPerApi(3);
                features.setPrioritySupport(false);
                break;
            case "PRO":
                features.setMaxDataPerApi(20);
                features.setPrioritySupport(true);
                break;
            case "ENTERPRISE":
                features.setMaxDataPerApi(Integer.MAX_VALUE);
                features.setPrioritySupport(true);
                break;
            default:
                features.setMaxDataPerApi(3);
                features.setPrioritySupport(false);
        }

        return features;
    }

    public UserSubscription getCurrentSubscription(User user) {
        return userSubscriptionRepository.findActiveSubscriptionByUser(user)
                .orElseGet(() -> {
                    log.warn("활성 구독을 찾을 수 없음, 무료 플랜으로 생성 - userId: {}", user.getUserId());
                    SubscriptionPlan freePlan = getFreePlan();
                    UserSubscription freeSubscription = UserSubscription.builder()
                            .user(user)
                            .plan(freePlan)
                            .planPaymentDate(LocalDateTime.now())
                            .build();
                    return userSubscriptionRepository.save(freeSubscription);
                });
    }

    public boolean checkRateLimit(User user) {
        UserSubscription subscription = getCurrentSubscription(user);
        RateLimitInfo rateLimitInfo = getRateLimitInfo(subscription.getPlan());
        
        String userId = user.getUserId();
        
        // 분당 제한 확인
        String minuteKey = "rate_limit:minute:" + userId;
        String minuteCount = redisTemplate.opsForValue().get(minuteKey);
        if (minuteCount != null && Integer.parseInt(minuteCount) >= rateLimitInfo.getPerMinute()) {
            log.warn("분당 Rate Limit 초과 - userId: {}, count: {}, limit: {}", 
                    userId, minuteCount, rateLimitInfo.getPerMinute());
            return false;
        }
        
        // 시간당 제한 확인
        String hourKey = "rate_limit:hour:" + userId;
        String hourCount = redisTemplate.opsForValue().get(hourKey);
        if (hourCount != null && Integer.parseInt(hourCount) >= rateLimitInfo.getPerHour()) {
            log.warn("시간당 Rate Limit 초과 - userId: {}, count: {}, limit: {}", 
                    userId, hourCount, rateLimitInfo.getPerHour());
            return false;
        }
        
        // 일일 제한 확인
        String dayKey = "rate_limit:day:" + userId;
        String dayCount = redisTemplate.opsForValue().get(dayKey);
        if (dayCount != null && Integer.parseInt(dayCount) >= rateLimitInfo.getPerDay()) {
            log.warn("일일 Rate Limit 초과 - userId: {}, count: {}, limit: {}", 
                    userId, dayCount, rateLimitInfo.getPerDay());
            return false;
        }
        
        return true;
    }

    public boolean checkApiUsageLimit(User user, String action) {
        if ("VIEW_USAGE".equals(action)) {
            return true; // 사용량 조회는 제한하지 않음
        }
        
        UserSubscription subscription = getCurrentSubscription(user);
        String userId = user.getUserId();
        
        // API 생성 관련 액션인 경우 생성된 API 개수 확인
        if ("CREATE_API".equals(action)) {
            String apiCountKey = "api_count:" + userId;
            String apiCount = redisTemplate.opsForValue().get(apiCountKey);
            int currentCount = apiCount != null ? Integer.parseInt(apiCount) : 0;
            
            if (currentCount >= subscription.getPlan().getMaxApiCount()) {
                log.warn("API 생성 한도 초과 - userId: {}, current: {}, limit: {}", 
                        userId, currentCount, subscription.getPlan().getMaxApiCount());
                return false;
            }
        }
        
        return true;
    }

    public void recordApiUsage(User user, String action) {
        String userId = user.getUserId();
        
        // Rate Limit 카운터 증가
        incrementRateLimitCounters(userId);
        
        // API 생성 카운터 증가
        if ("CREATE_API".equals(action)) {
            String apiCountKey = "api_count:" + userId;
            redisTemplate.opsForValue().increment(apiCountKey);
            redisTemplate.expire(apiCountKey, Duration.ofDays(30)); // 30일 TTL
        }
        
        // 사용량 기록 (통계용)
        String usageKey = "usage:" + userId + ":" + action + ":" + LocalDateTime.now().toLocalDate();
        redisTemplate.opsForValue().increment(usageKey);
        redisTemplate.expire(usageKey, Duration.ofDays(90)); // 90일 TTL
        
        log.debug("API 사용량 기록 - userId: {}, action: {}", userId, action);
    }

    private void incrementRateLimitCounters(String userId) {
        LocalDateTime now = LocalDateTime.now();
        
        // 분당 카운터
        String minuteKey = "rate_limit:minute:" + userId;
        redisTemplate.opsForValue().increment(minuteKey);
        redisTemplate.expire(minuteKey, Duration.ofMinutes(1));
        
        // 시간당 카운터
        String hourKey = "rate_limit:hour:" + userId;
        redisTemplate.opsForValue().increment(hourKey);
        redisTemplate.expire(hourKey, Duration.ofHours(1));
        
        // 일일 카운터
        String dayKey = "rate_limit:day:" + userId;
        redisTemplate.opsForValue().increment(dayKey);
        redisTemplate.expire(dayKey, Duration.ofDays(1));
    }

    @Data
    public static class RateLimitInfo {
        private final Integer perMinute;
        private final Integer perHour;
        private final Integer perDay;
    }

    @Data
    public static class PlanFeatures {
        private Integer maxApiCount;
        private Integer maxDataPerApi;
        private boolean apiSharingAllowed;
        private boolean prioritySupport;
    }
}