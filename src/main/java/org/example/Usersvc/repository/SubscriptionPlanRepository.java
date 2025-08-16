package org.example.Usersvc.repository;

import org.example.Usersvc.domain.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 구독 플랜 레포지토리
 */
@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    
    /**
     * 플랜명으로 조회
     */
    Optional<SubscriptionPlan> findByPlanName(String planName);
    
    /**
     * 활성화된 플랜 목록 조회
     */
    @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.isActive = true ORDER BY sp.monthlyPrice ASC")
    List<SubscriptionPlan> findActivePlansOrderByPrice();
    
    /**
     * 기본 무료 플랜 조회
     */
    @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.planName = 'FREE' AND sp.isActive = true")
    Optional<SubscriptionPlan> findFreePlan();

    /**
     * 활성화된 플랜을 가격순으로 조회
     */
    List<SubscriptionPlan> findByIsActiveTrueOrderByMonthlyPrice();

    /**
     * 플랜명과 활성화 상태로 조회
     */
    Optional<SubscriptionPlan> findByPlanNameAndIsActiveTrue(String planName);
}