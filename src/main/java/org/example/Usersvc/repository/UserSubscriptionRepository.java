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
           "WHERE us.user = :user AND us.isActive = true " +
           "AND (us.expiresAt IS NULL OR us.expiresAt > :now)")
    Optional<UserSubscription> findActiveSubscriptionByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    /**
     * 사용자의 현재 활성 구독 조회 (편의 메소드)
     */
    default Optional<UserSubscription> findActiveSubscriptionByUser(User user) {
        return findActiveSubscriptionByUser(user, LocalDateTime.now());
    }
    
    /**
     * 만료된 구독 목록 조회
     */
    @Query("SELECT us FROM UserSubscription us " +
           "WHERE us.isActive = true AND us.expiresAt < :now")
    java.util.List<UserSubscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);
}