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
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Integer> {
    
    /**
     * 플랜명으로 조회
     */
    Optional<SubscriptionPlan> findByPlanName(String planName);
    
    /**
     * 기본 무료 플랜 조회
     */
    @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.planName = 'Free'")
    Optional<SubscriptionPlan> findFreePlan();

    /**
     * 모든 플랜을 가격순으로 조회
     */
    List<SubscriptionPlan> findAllByOrderByPrice();
}