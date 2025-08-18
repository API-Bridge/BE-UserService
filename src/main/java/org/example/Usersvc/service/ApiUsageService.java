package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.ApiUsageRecord;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.repository.ApiUsageRecordRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * API 사용량 추적 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiUsageService {
    
    private final ApiUsageRecordRepository apiUsageRecordRepository;
    
    /**
     * API 호출 기록 저장 (비동기)
     */
    @Async
    @Transactional
    public void recordApiUsage(User user, String apiEndpoint, int requestCount) {
        try {
            ApiUsageRecord record = ApiUsageRecord.builder()
                    .user(user)
                    .apiEndpoint(apiEndpoint)
                    .requestCount(requestCount)
                    .recordDate(java.time.LocalDate.now())
                    .build();
            
            apiUsageRecordRepository.save(record);
            log.debug("API usage recorded for user: {}, endpoint: {}", user.getUserId(), apiEndpoint);
            
        } catch (Exception e) {
            log.error("Failed to record API usage for user: {}, endpoint: {}", user.getUserId(), apiEndpoint, e);
        }
    }
    
    /**
     * 사용자의 분당 API 사용량 조회
     */
    @Transactional(readOnly = true)
    public long getUserMinuteUsage(User user) {
        return apiUsageRecordRepository.countByUserLastMinute(user);
    }
    
    /**
     * 사용자의 시간당 API 사용량 조회
     */
    @Transactional(readOnly = true)
    public long getUserHourUsage(User user) {
        return apiUsageRecordRepository.countByUserLastHour(user);
    }
    
    /**
     * 사용자의 일일 API 사용량 조회
     */
    @Transactional(readOnly = true)
    public long getUserDailyUsage(User user) {
        return apiUsageRecordRepository.countByUserToday(user);
    }
    
    /**
     * 특정 기간 내 사용량 조회
     */
    @Transactional(readOnly = true)
    public long getUserUsageInPeriod(User user, LocalDateTime startTime, LocalDateTime endTime) {
        return apiUsageRecordRepository.countByUserAndPeriod(user, startTime, endTime);
    }
    
    /**
     * 특정 API 엔드포인트 사용량 조회
     */
    @Transactional(readOnly = true)
    public long getUserEndpointUsage(User user, String endpoint, LocalDateTime startTime, LocalDateTime endTime) {
        return apiUsageRecordRepository.countByUserAndEndpointAndPeriod(user, endpoint, startTime, endTime);
    }
}