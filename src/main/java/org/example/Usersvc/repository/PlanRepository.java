package org.example.Usersvc.repository;

import org.example.Usersvc.domain.Plan;
import org.example.Usersvc.domain.PlanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Plan 레포지토리
 */
@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {
    
    /**
     * 플랜 타입으로 플랜 조회
     */
    Optional<Plan> findByPlanType(PlanType planType);
    
    /**
     * 플랜 타입 존재 여부 확인
     */
    boolean existsByPlanType(PlanType planType);
    
    /**
     * 플랜명으로 플랜 조회 (호환성을 위한 유지)
     * @deprecated planType 사용 권장
     */
    @Deprecated
    default Optional<Plan> findByPlanName(String planName) {
        try {
            PlanType planType = PlanType.fromString(planName);
            return findByPlanType(planType);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
