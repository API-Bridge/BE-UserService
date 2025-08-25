package org.example.Usersvc.repository;

import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 사용자 구독 레포지토리
 */
@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    
    /**
     * 사용자의 활성 구독 조회
     */
    @Query("SELECT us FROM UserSubscription us " +
           "JOIN FETCH us.plan " +
           "WHERE us.user = :user " +
           "ORDER BY us.subscriptionId DESC " +
           "LIMIT 1")
    Optional<UserSubscription> findActiveSubscriptionByUser(@Param("user") User user);
    
    /**
     * 비활성 구독 목록 조회 (만료된 구독 대신)
     */
    @Query("SELECT us1 FROM UserSubscription us1 " +
           "WHERE NOT EXISTS (" +
           "  SELECT 1 FROM UserSubscription us2 " +
           "  WHERE us2.user = us1.user " +
           "  AND us2.subscriptionId > us1.subscriptionId" +
           ")")
    java.util.List<UserSubscription> findInactiveSubscriptions();
    
    /**
     * 데이터 무결성 검증: 다중 활성 구독을 가진 사용자 조회
     */
    @Query("SELECT us.user FROM UserSubscription us " +
           "GROUP BY us.user " +
           "HAVING COUNT(us) > 1")
    java.util.List<User> findUsersWithMultipleActiveSubscriptions();
    
    /**
     * 사용자별 모든 구독 조회 (활성/비활성 포함, 최신순)
     */
    @Query("SELECT us FROM UserSubscription us " +
           "JOIN FETCH us.plan " +
           "WHERE us.user = :user " +
           "ORDER BY us.planUpdateDate DESC, us.planPaymentDate DESC")
    java.util.List<UserSubscription> findAllSubscriptionsByUserOrderByLatest(@Param("user") User user);
    
    /**
     * 특정 사용자의 구독 개수 조회
     */
    @Query("SELECT COUNT(us) FROM UserSubscription us WHERE us.user = :user")
    long countSubscriptionsByUser(@Param("user") User user);
    
    /**
     * 특정 사용자의 활성 구독 개수 조회
     */
    @Query("SELECT COUNT(us) FROM UserSubscription us WHERE us.user = :user")
    long countActiveSubscriptionsByUser(@Param("user") User user);
    
    /**
     * 전체 시스템 구독 통계 조회
     */
    @Query("SELECT " +
           "COUNT(DISTINCT us.user) as totalUsers, " +
           "COUNT(us) as totalSubscriptions, " +
           "COUNT(us) as activeSubscriptions, " +
           "0 as inactiveSubscriptions " +
           "FROM UserSubscription us")
    Object[] getSubscriptionStatistics();
    
    /**
     * 플랜별 구독 수 조회
     */
    @Query("SELECT COUNT(us) FROM UserSubscription us WHERE us.plan = :plan")
    long countByPlan(@Param("plan") org.example.Usersvc.domain.Plan plan);
    
}