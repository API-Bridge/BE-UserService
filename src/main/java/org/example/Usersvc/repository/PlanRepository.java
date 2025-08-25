package org.example.Usersvc.repository;

import org.example.Usersvc.domain.Plan;
import org.example.Usersvc.domain.PlanName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Plan 레포지토리
 */
@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {
    
    /**
     * 플랜 이름으로 플랜 조회
     */
    @Query("SELECT p FROM Plan p WHERE p.planName = :planName")
    Optional<Plan> findByPlanName(@Param("planName") PlanName planName);
    
    /**
     * 플랜 이름 존재 여부 확인
     */
    @Query("SELECT COUNT(p) > 0 FROM Plan p WHERE p.planName = :planName")
    boolean existsByPlanName(@Param("planName") PlanName planName);
}
