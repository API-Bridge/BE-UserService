package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 Rate Limiting 서비스
 */
@Slf4j
@Service
@Profile("!dev")
@RequiredArgsConstructor
public class RateLimitService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final UserSubscriptionService userSubscriptionService;
    
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final DateTimeFormatter MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 분당 호출 제한 확인
     */
    public boolean isAllowedPerMinute(User user) {
        UserSubscription subscription = userSubscriptionService.getActiveSubscription(user);
        int limit = subscription.getPlan().getRateLimitPerMinute();
        
        String key = generateMinuteKey(user.getUserId());
        return checkAndIncrementCounter(key, limit, 60);
    }
    
    /**
     * 시간당 호출 제한 확인
     */
    public boolean isAllowedPerHour(User user) {
        UserSubscription subscription = userSubscriptionService.getActiveSubscription(user);
        int limit = subscription.getPlan().getRateLimitPerHour();
        
        String key = generateHourKey(user.getUserId());
        return checkAndIncrementCounter(key, limit, 3600);
    }
    
    /**
     * 일일 호출 제한 확인
     */
    public boolean isAllowedPerDay(User user) {
        UserSubscription subscription = userSubscriptionService.getActiveSubscription(user);
        int limit = subscription.getPlan().getRateLimitPerDay();
        
        String key = generateDayKey(user.getUserId());
        return checkAndIncrementCounter(key, limit, 86400);
    }
    
    /**
     * 현재 분당 사용량 조회
     */
    public long getCurrentMinuteUsage(User user) {
        String key = generateMinuteKey(user.getUserId());
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count) : 0;
    }
    
    /**
     * 현재 시간당 사용량 조회
     */
    public long getCurrentHourUsage(User user) {
        String key = generateHourKey(user.getUserId());
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count) : 0;
    }
    
    /**
     * 현재 일일 사용량 조회
     */
    public long getCurrentDayUsage(User user) {
        String key = generateDayKey(user.getUserId());
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count) : 0;
    }
    
    /**
     * 카운터 확인 및 증가
     */
    private boolean checkAndIncrementCounter(String key, int limit, int ttlSeconds) {
        try {
            String currentValue = redisTemplate.opsForValue().get(key);
            
            if (currentValue == null) {
                // 첫 요청인 경우
                redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
                return true;
            }
            
            long currentCount = Long.parseLong(currentValue);
            if (currentCount >= limit) {
                log.warn("Rate limit exceeded for key: {}, current: {}, limit: {}", key, currentCount, limit);
                return false;
            }
            
            // 카운터 증가
            redisTemplate.opsForValue().increment(key);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            // Redis 오류 시 기본적으로 허용 (graceful degradation)
            return true;
        }
    }
    
    /**
     * 분 단위 키 생성
     */
    private String generateMinuteKey(String userId) {
        String timeKey = LocalDateTime.now().format(MINUTE_FORMAT);
        return RATE_LIMIT_PREFIX + "minute:" + userId + ":" + timeKey;
    }
    
    /**
     * 시간 단위 키 생성
     */
    private String generateHourKey(String userId) {
        String timeKey = LocalDateTime.now().format(HOUR_FORMAT);
        return RATE_LIMIT_PREFIX + "hour:" + userId + ":" + timeKey;
    }
    
    /**
     * 일 단위 키 생성
     */
    private String generateDayKey(String userId) {
        String timeKey = LocalDateTime.now().format(DAY_FORMAT);
        return RATE_LIMIT_PREFIX + "day:" + userId + ":" + timeKey;
    }
}