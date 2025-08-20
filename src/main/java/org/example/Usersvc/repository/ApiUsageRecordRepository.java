package org.example.Usersvc.repository;

import org.example.Usersvc.domain.ApiUsageRecord;
import org.example.Usersvc.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * API 사용량 기록 레포지토리
 */
@Repository
public interface ApiUsageRecordRepository extends JpaRepository<ApiUsageRecord, String> {
    
    /**
     * 특정 기간 내 사용자의 API 호출 횟수 조회
     */
    @Query("SELECT COUNT(a) FROM ApiUsageRecord a " +
           "WHERE a.user = :user AND a.recordDate BETWEEN :startTime AND :endTime")
    long countByUserAndPeriod(@Param("user") User user, 
                             @Param("startTime") LocalDateTime startTime, 
                             @Param("endTime") LocalDateTime endTime);
    
    /**
     * 분당 호출 횟수 조회
     */
    default long countByUserLastMinute(User user) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        return countByUserAndPeriod(user, oneMinuteAgo, LocalDateTime.now());
    }
    
    /**
     * 시간당 호출 횟수 조회
     */
    default long countByUserLastHour(User user) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return countByUserAndPeriod(user, oneHourAgo, LocalDateTime.now());
    }
    
    /**
     * 일일 호출 횟수 조회
     */
    default long countByUserToday(User user) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return countByUserAndPeriod(user, startOfDay, LocalDateTime.now());
    }
    
    /**
     * 특정 API 엔드포인트의 사용량 조회
     */
    @Query("SELECT COUNT(a) FROM ApiUsageRecord a " +
           "WHERE a.user = :user AND a.apiEndpoint = :endpoint " +
           "AND a.recordDate BETWEEN :startTime AND :endTime")
    long countByUserAndEndpointAndPeriod(@Param("user") User user,
                                        @Param("endpoint") String endpoint,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 사용자 + API 엔드포인트 + 날짜로 기록 조회
     */
    @Query("SELECT a FROM ApiUsageRecord a WHERE a.user.userId = :userId AND a.apiEndpoint = :endpoint AND a.recordDate = :recordDate")
    Optional<ApiUsageRecord> findByUserIdAndApiEndpointAndRecordDate(
            @Param("userId") String userId, 
            @Param("endpoint") String apiEndpoint, 
            @Param("recordDate") LocalDate recordDate);

    /**
     * 사용자의 특정 기간 총 요청 수 합계
     */
    @Query("SELECT COALESCE(SUM(a.requestCount), 0) FROM ApiUsageRecord a " +
           "WHERE a.user.userId = :userId AND a.recordDate >= :startDate AND a.recordDate <= :endDate")
    long sumRequestCountByUserIdAndDateRange(@Param("userId") String userId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
}