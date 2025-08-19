package org.example.Usersvc.repository;

import org.example.Usersvc.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Plan 레포지토리
 */
@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {
    
    /**
     * 플랜명으로 플랜 조회
     */
    Optional<Plan> findByPlanName(String planName);
    
    /**
     * 플랜명 존재 여부 확인
     */
    boolean existsByPlanName(String planName);
}
