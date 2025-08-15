package org.example.Usersvc.service;

import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.User;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 개발 환경용 인메모리 Rate Limiting 서비스
 */
@Slf4j
@Service
@Profile("dev")
public class DevRateLimitService {
    
    // 메모리 기반 카운터 (개발용)
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    
    // 개발 환경에서는 관대한 제한 적용
    private static final int DEV_RATE_LIMIT_PER_MINUTE = 1000;
    private static final int DEV_RATE_LIMIT_PER_HOUR = 10000;
    private static final int DEV_RATE_LIMIT_PER_DAY = 100000;
    
    /**
     * 분당 호출 제한 확인 (개발용)
     */
    public boolean isAllowedPerMinute(User user) {
        return checkLimit(user.getUserId(), "minute", DEV_RATE_LIMIT_PER_MINUTE);
    }
    
    /**
     * 시간당 호출 제한 확인 (개발용)
     */
    public boolean isAllowedPerHour(User user) {
        return checkLimit(user.getUserId(), "hour", DEV_RATE_LIMIT_PER_HOUR);
    }
    
    /**
     * 일일 호출 제한 확인 (개발용)
     */
    public boolean isAllowedPerDay(User user) {
        return checkLimit(user.getUserId(), "day", DEV_RATE_LIMIT_PER_DAY);
    }
    
    /**
     * 현재 분당 사용량 조회
     */
    public long getCurrentMinuteUsage(User user) {
        return getUsage(user.getUserId(), "minute");
    }
    
    /**
     * 현재 시간당 사용량 조회
     */
    public long getCurrentHourUsage(User user) {
        return getUsage(user.getUserId(), "hour");
    }
    
    /**
     * 현재 일일 사용량 조회
     */
    public long getCurrentDayUsage(User user) {
        return getUsage(user.getUserId(), "day");
    }
    
    /**
     * 제한 확인 (간단한 메모리 기반)
     */
    private boolean checkLimit(String userId, String period, int limit) {
        String key = userId + ":" + period;
        AtomicLong counter = counters.computeIfAbsent(key, k -> new AtomicLong(0));
        
        long current = counter.incrementAndGet();
        log.debug("Dev rate limit check - User: {}, Period: {}, Current: {}, Limit: {}", 
                 userId, period, current, limit);
        
        return current <= limit;
    }
    
    /**
     * 사용량 조회
     */
    private long getUsage(String userId, String period) {
        String key = userId + ":" + period;
        AtomicLong counter = counters.get(key);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * 카운터 초기화 (테스트용)
     */
    public void resetCounters() {
        counters.clear();
        log.info("Dev rate limit counters reset");
    }
}