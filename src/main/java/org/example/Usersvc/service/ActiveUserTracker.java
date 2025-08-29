package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.ActiveUserRecord;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.repository.ActiveUserRecordRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * 활성 사용자 추적 서비스
 * Redis HyperLogLog를 사용하여 DAU/WAU/MAU를 효율적으로 측정
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ActiveUserTracker {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ActiveUserRecordRepository activeUserRecordRepository;
    
    private static final String DAU_KEY_PREFIX = "dau:";
    private static final String WAU_KEY_PREFIX = "wau:";
    private static final String MAU_KEY_PREFIX = "mau:";
    
    /**
     * 사용자 활동을 기록합니다
     * 
     * @param user 사용자
     * @param activityType 활동 유형
     */
    public void track(User user, String activityType) {
        String userId = user.getUserId();
        LocalDate today = LocalDate.now();
        
        try {
            // Redis HyperLogLog에 사용자 ID 추가
            addToHyperLogLog(userId, today);
            
            // 데이터베이스에 백업 기록 (하루에 한 번만)
            recordToDatabaseIfNotExists(user, activityType, today);
            
            log.debug("사용자 활동 기록됨 - userId: {}, activityType: {}", userId, activityType);
            
        } catch (Exception e) {
            log.error("사용자 활동 기록 실패 - userId: {}, activityType: {}", userId, activityType, e);
        }
    }
    
    /**
     * 사용자 활동 기록 (기본 활동 유형: API_CALL)
     * 
     * @param user 사용자
     */
    public void track(User user) {
        track(user, ActiveUserRecord.ActivityType.API_CALL.getValue());
    }
    
    /**
     * 특정 날짜의 DAU 조회
     * 
     * @param date 조회 날짜
     * @return DAU 수
     */
    public long getDailyActiveUsers(LocalDate date) {
        try {
            String key = DAU_KEY_PREFIX + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            Long count = redisTemplate.opsForHyperLogLog().size(key);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("DAU 조회 실패 - date: {}", date, e);
            // Redis 실패 시 데이터베이스 백업 사용
            return activeUserRecordRepository.countDistinctActiveUsersByDate(date);
        }
    }
    
    /**
     * 오늘의 DAU 조회
     * 
     * @return 오늘의 DAU 수
     */
    public long getTodayActiveUsers() {
        return getDailyActiveUsers(LocalDate.now());
    }
    
    /**
     * 현재 주의 WAU 조회
     * 
     * @return WAU 수
     */
    public long getWeeklyActiveUsers() {
        try {
            LocalDate today = LocalDate.now();
            String weekKey = getWeekKey(today);
            String key = WAU_KEY_PREFIX + weekKey;
            Long count = redisTemplate.opsForHyperLogLog().size(key);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("WAU 조회 실패", e);
            // Redis 실패 시 데이터베이스 백업 사용
            LocalDate today = LocalDate.now();
            LocalDate startOfWeek = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
            return activeUserRecordRepository.countDistinctActiveUsersByDateRange(startOfWeek, today);
        }
    }
    
    /**
     * 현재 월의 MAU 조회
     * 
     * @return MAU 수
     */
    public long getMonthlyActiveUsers() {
        try {
            LocalDate today = LocalDate.now();
            String monthKey = getMonthKey(today);
            String key = MAU_KEY_PREFIX + monthKey;
            Long count = redisTemplate.opsForHyperLogLog().size(key);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("MAU 조회 실패", e);
            // Redis 실패 시 데이터베이스 백업 사용
            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = today.withDayOfMonth(1);
            return activeUserRecordRepository.countDistinctActiveUsersByDateRange(startOfMonth, today);
        }
    }
    
    /**
     * Redis HyperLogLog에 사용자 ID 추가
     */
    private void addToHyperLogLog(String userId, LocalDate date) {
        // 일별 키
        String dauKey = DAU_KEY_PREFIX + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        redisTemplate.opsForHyperLogLog().add(dauKey, userId);
        
        // 주별 키
        String weekKey = getWeekKey(date);
        String wauKey = WAU_KEY_PREFIX + weekKey;
        redisTemplate.opsForHyperLogLog().add(wauKey, userId);
        
        // 월별 키
        String monthKey = getMonthKey(date);
        String mauKey = MAU_KEY_PREFIX + monthKey;
        redisTemplate.opsForHyperLogLog().add(mauKey, userId);
        
        // TTL 설정 (자동 정리)
        redisTemplate.expire(dauKey, java.time.Duration.ofDays(7)); // DAU 키는 7일 후 삭제
        redisTemplate.expire(wauKey, java.time.Duration.ofDays(30)); // WAU 키는 30일 후 삭제
        redisTemplate.expire(mauKey, java.time.Duration.ofDays(90)); // MAU 키는 90일 후 삭제
    }
    
    /**
     * 데이터베이스에 백업 기록 (하루에 한 번만)
     */
    private void recordToDatabaseIfNotExists(User user, String activityType, LocalDate date) {
        try {
            // 오늘 이미 기록되어 있는지 확인
            if (!activeUserRecordRepository.existsByUserIdAndActiveDate(user.getUserId(), date)) {
                ActiveUserRecord record = ActiveUserRecord.builder()
                        .user(user)
                        .activityType(activityType)
                        .activeDate(date)
                        .build();
                activeUserRecordRepository.save(record);
            }
        } catch (Exception e) {
            log.warn("데이터베이스 백업 기록 실패 - userId: {}, date: {}", user.getUserId(), date, e);
            // 데이터베이스 실패가 전체 처리를 방해하지 않도록 예외를 삼킴
        }
    }
    
    /**
     * 주차 키 생성 (YYYY-WW 형태)
     */
    private String getWeekKey(LocalDate date) {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
        int year = date.get(weekFields.weekBasedYear());
        return String.format("%d-W%02d", year, weekOfYear);
    }
    
    /**
     * 월 키 생성 (YYYY-MM 형태)
     */
    private String getMonthKey(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
    
    /**
     * 개발/테스트용 카운터 초기화
     */
    public void resetCounters(String pattern) {
        try {
            redisTemplate.delete(redisTemplate.keys(pattern + "*"));
            log.info("Redis 카운터 초기화됨 - pattern: {}", pattern);
        } catch (Exception e) {
            log.error("Redis 카운터 초기화 실패 - pattern: {}", pattern, e);
        }
    }
    
    /**
     * 모든 카운터 초기화 (개발/테스트용)
     */
    public void resetAllCounters() {
        resetCounters(DAU_KEY_PREFIX);
        resetCounters(WAU_KEY_PREFIX);
        resetCounters(MAU_KEY_PREFIX);
    }
}