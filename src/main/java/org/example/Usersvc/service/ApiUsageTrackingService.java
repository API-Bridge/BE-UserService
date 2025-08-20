package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.ApiUsageRecord;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.repository.ApiUsageRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * API 사용량 추적 서비스
 * 
 * 실제 API 호출을 추적하고 사용량을 데이터베이스에 기록합니다.
 * - 실시간 사용량 추적 (분/시간/일별)
 * - 데이터베이스 기반 영구 저장
 * - 메모리 캐시로 성능 최적화
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ApiUsageTrackingService {

    private final ApiUsageRecordRepository apiUsageRecordRepository;
    
    // 메모리 기반 실시간 카운터 (성능 최적화용)
    private final ConcurrentHashMap<String, AtomicLong> minuteCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> hourCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> dayCounters = new ConcurrentHashMap<>();
    
    // 카운터 마지막 초기화 시간
    private final ConcurrentHashMap<String, LocalDateTime> lastResetTimes = new ConcurrentHashMap<>();

    /**
     * API 호출을 기록합니다.
     * 
     * @param user 사용자
     * @param apiEndpoint 호출된 API 엔드포인트
     * @param requestCount 요청 횟수 (기본값: 1)
     */
    public void recordApiCall(User user, String apiEndpoint, int requestCount) {
        String userId = user.getUserId();
        
        try {
            // 실시간 카운터 업데이트
            updateRealTimeCounters(userId, requestCount);
            
            // 데이터베이스 기록 (일별 누적)
            updateDailyRecord(user, apiEndpoint, requestCount);
            
            log.debug("API 호출 기록됨 - userId: {}, endpoint: {}, count: {}", 
                    userId, apiEndpoint, requestCount);
                    
        } catch (Exception e) {
            log.error("API 사용량 기록 실패 - userId: {}, endpoint: {}", userId, apiEndpoint, e);
            // 기록 실패가 전체 API 호출을 방해하지 않도록 예외를 삼킴
        }
    }

    /**
     * API 호출을 기록합니다 (기본 1회).
     * 
     * @param user 사용자
     * @param apiEndpoint 호출된 API 엔드포인트
     */
    public void recordApiCall(User user, String apiEndpoint) {
        recordApiCall(user, apiEndpoint, 1);
    }

    /**
     * 현재 분당 사용량 조회
     * 
     * @param user 사용자
     * @return 현재 분 사용량
     */
    public long getCurrentMinuteUsage(User user) {
        String key = getMinuteKey(user.getUserId());
        cleanupExpiredCounters(user.getUserId());
        return minuteCounters.getOrDefault(key, new AtomicLong(0)).get();
    }

    /**
     * 현재 시간당 사용량 조회
     * 
     * @param user 사용자
     * @return 현재 시간 사용량
     */
    public long getCurrentHourUsage(User user) {
        String key = getHourKey(user.getUserId());
        cleanupExpiredCounters(user.getUserId());
        return hourCounters.getOrDefault(key, new AtomicLong(0)).get();
    }

    /**
     * 현재 일일 사용량 조회
     * 
     * @param user 사용자
     * @return 현재 일 사용량
     */
    public long getCurrentDayUsage(User user) {
        String key = getDayKey(user.getUserId());
        cleanupExpiredCounters(user.getUserId());
        return dayCounters.getOrDefault(key, new AtomicLong(0)).get();
    }

    /**
     * 월간 사용량 조회 (데이터베이스 기반)
     * 
     * @param user 사용자
     * @return 이번 달 총 사용량
     */
    @Transactional(readOnly = true)
    public long getMonthlyUsage(User user) {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        
        return apiUsageRecordRepository.sumRequestCountByUserIdAndDateRange(
                user.getUserId(), startOfMonth, endOfMonth);
    }

    /**
     * 특정 기간의 사용량 조회
     * 
     * @param user 사용자
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 기간별 총 사용량
     */
    @Transactional(readOnly = true)
    public long getUsageByDateRange(User user, LocalDate startDate, LocalDate endDate) {
        return apiUsageRecordRepository.sumRequestCountByUserIdAndDateRange(
                user.getUserId(), startDate, endDate);
    }

    /**
     * 실시간 카운터 업데이트
     */
    private void updateRealTimeCounters(String userId, int requestCount) {
        LocalDateTime now = LocalDateTime.now();
        
        // 분별 카운터
        String minuteKey = getMinuteKey(userId);
        minuteCounters.computeIfAbsent(minuteKey, k -> new AtomicLong(0))
                     .addAndGet(requestCount);
        
        // 시간별 카운터
        String hourKey = getHourKey(userId);
        hourCounters.computeIfAbsent(hourKey, k -> new AtomicLong(0))
                   .addAndGet(requestCount);
        
        // 일별 카운터
        String dayKey = getDayKey(userId);
        dayCounters.computeIfAbsent(dayKey, k -> new AtomicLong(0))
                  .addAndGet(requestCount);
        
        // 마지막 업데이트 시간 기록
        lastResetTimes.put(userId, now);
    }

    /**
     * 데이터베이스에 일별 기록 업데이트
     */
    private void updateDailyRecord(User user, String apiEndpoint, int requestCount) {
        LocalDate today = LocalDate.now();
        
        Optional<ApiUsageRecord> existingRecord = 
                apiUsageRecordRepository.findByUserIdAndApiEndpointAndRecordDate(
                        user.getUserId(), apiEndpoint, today);
        
        if (existingRecord.isPresent()) {
            // 기존 기록 업데이트
            ApiUsageRecord record = existingRecord.get();
            record.incrementRequestCount(requestCount);
            apiUsageRecordRepository.save(record);
        } else {
            // 새 기록 생성
            ApiUsageRecord newRecord = ApiUsageRecord.builder()
                    .user(user)
                    .apiEndpoint(apiEndpoint)
                    .requestCount(requestCount)
                    .recordDate(today)
                    .build();
            apiUsageRecordRepository.save(newRecord);
        }
    }

    /**
     * 만료된 카운터 정리
     */
    private void cleanupExpiredCounters(String userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastReset = lastResetTimes.get(userId);
        
        if (lastReset != null) {
            // 1분이 지난 분별 카운터 정리
            if (ChronoUnit.MINUTES.between(lastReset, now) >= 1) {
                String oldMinuteKey = getMinuteKey(userId, lastReset);
                minuteCounters.remove(oldMinuteKey);
            }
            
            // 1시간이 지난 시간별 카운터 정리
            if (ChronoUnit.HOURS.between(lastReset, now) >= 1) {
                String oldHourKey = getHourKey(userId, lastReset);
                hourCounters.remove(oldHourKey);
            }
            
            // 1일이 지난 일별 카운터 정리
            if (ChronoUnit.DAYS.between(lastReset, now) >= 1) {
                String oldDayKey = getDayKey(userId, lastReset);
                dayCounters.remove(oldDayKey);
            }
        }
    }

    /**
     * 분별 카운터 키 생성
     */
    private String getMinuteKey(String userId) {
        return getMinuteKey(userId, LocalDateTime.now());
    }
    
    private String getMinuteKey(String userId, LocalDateTime time) {
        return String.format("%s:minute:%d:%d:%d", 
                userId, time.getHour(), time.getMinute(), time.getSecond() / 60);
    }

    /**
     * 시간별 카운터 키 생성
     */
    private String getHourKey(String userId) {
        return getHourKey(userId, LocalDateTime.now());
    }
    
    private String getHourKey(String userId, LocalDateTime time) {
        return String.format("%s:hour:%d:%d", 
                userId, time.getDayOfYear(), time.getHour());
    }

    /**
     * 일별 카운터 키 생성
     */
    private String getDayKey(String userId) {
        return getDayKey(userId, LocalDateTime.now());
    }
    
    private String getDayKey(String userId, LocalDateTime time) {
        return String.format("%s:day:%d:%d", 
                userId, time.getYear(), time.getDayOfYear());
    }

    /**
     * 개발/테스트용 카운터 초기화
     */
    public void resetCounters(String userId) {
        minuteCounters.entrySet().removeIf(entry -> entry.getKey().startsWith(userId));
        hourCounters.entrySet().removeIf(entry -> entry.getKey().startsWith(userId));
        dayCounters.entrySet().removeIf(entry -> entry.getKey().startsWith(userId));
        lastResetTimes.remove(userId);
        
        log.info("사용량 카운터 초기화됨 - userId: {}", userId);
    }
}