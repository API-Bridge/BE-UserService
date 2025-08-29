package org.example.Usersvc.repository;

import org.example.Usersvc.domain.ActiveUserRecord;
import org.example.Usersvc.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 활성 사용자 기록 레포지토리
 * DAU/MAU 측정을 위한 사용자 활동 기록 관리
 */
@Repository
public interface ActiveUserRecordRepository extends JpaRepository<ActiveUserRecord, String> {
    
    /**
     * 특정 날짜의 활성 사용자 기록 존재 여부 확인
     */
    @Query("SELECT COUNT(a) > 0 FROM ActiveUserRecord a " +
           "WHERE a.user.userId = :userId AND a.activeDate = :date")
    boolean existsByUserIdAndActiveDate(@Param("userId") String userId, 
                                      @Param("date") LocalDate date);
    
    /**
     * 특정 기간 내 활성 사용자 기록 조회
     */
    @Query("SELECT a FROM ActiveUserRecord a " +
           "WHERE a.user.userId = :userId AND a.activeDate BETWEEN :startDate AND :endDate")
    List<ActiveUserRecord> findByUserIdAndDateRange(@Param("userId") String userId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);
    
    /**
     * 사용자 + 활동 유형 + 날짜로 기록 조회
     */
    @Query("SELECT a FROM ActiveUserRecord a " +
           "WHERE a.user.userId = :userId AND a.activityType = :activityType AND a.activeDate = :activeDate")
    Optional<ActiveUserRecord> findByUserIdAndActivityTypeAndActiveDate(
            @Param("userId") String userId, 
            @Param("activityType") String activityType, 
            @Param("activeDate") LocalDate activeDate);
    
    /**
     * 특정 날짜의 고유 활성 사용자 수 조회 (DB 백업용)
     */
    @Query("SELECT COUNT(DISTINCT a.user.userId) FROM ActiveUserRecord a " +
           "WHERE a.activeDate = :date")
    long countDistinctActiveUsersByDate(@Param("date") LocalDate date);
    
    /**
     * 특정 기간의 고유 활성 사용자 수 조회 (DB 백업용)
     */
    @Query("SELECT COUNT(DISTINCT a.user.userId) FROM ActiveUserRecord a " +
           "WHERE a.activeDate BETWEEN :startDate AND :endDate")
    long countDistinctActiveUsersByDateRange(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
    
    /**
     * 특정 날짜의 활동 유형별 활성 사용자 수 조회
     */
    @Query("SELECT COUNT(DISTINCT a.user.userId) FROM ActiveUserRecord a " +
           "WHERE a.activeDate = :date AND a.activityType = :activityType")
    long countDistinctActiveUsersByDateAndActivityType(@Param("date") LocalDate date,
                                                     @Param("activityType") String activityType);
}