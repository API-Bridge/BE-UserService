package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.PlanType;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.repository.CustomApiRepository;
import org.example.Usersvc.repository.SharedApiRepository;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 플랜별 제한사항 검증 서비스
 * 
 * 사용자의 현재 플랜에 따른 각종 제한사항을 검증하고 관리합니다.
 * - Custom API 생성 개수 제한
 * - 공유 API 개수 제한  
 * - 데이터 묶음 개수 제한
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PlanLimitValidationService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final CustomApiRepository customApiRepository;
    private final SharedApiRepository sharedApiRepository;

    /**
     * 사용자가 새로운 Custom API를 생성할 수 있는지 검증
     * 
     * @param user 검증할 사용자
     * @return 생성 가능하면 true, 제한 초과 시 false
     */
    public boolean canCreateCustomApi(User user) {
        PlanType planType = getUserPlanType(user);
        int currentCount = (int) customApiRepository.countByUserId(user.getUserId());
        int maxAllowed = planType.getMaxCustomApiCount();
        
        boolean canCreate = currentCount < maxAllowed;
        
        log.debug("Custom API 생성 가능 여부 - userId: {}, plan: {}, current: {}, max: {}, canCreate: {}", 
                user.getUserId(), planType, currentCount, maxAllowed, canCreate);
                
        return canCreate;
    }

    /**
     * 사용자가 새로운 API를 공유할 수 있는지 검증
     * 
     * @param user 검증할 사용자
     * @return 공유 가능하면 true, 제한 초과 시 false
     */
    public boolean canShareApi(User user) {
        PlanType planType = getUserPlanType(user);
        int currentCount = (int) sharedApiRepository.countByCreatorIdAndIsActiveTrue(user.getUserId());
        int maxAllowed = planType.getMaxSharedApiCount();
        
        boolean canShare = currentCount < maxAllowed;
        
        log.debug("API 공유 가능 여부 - userId: {}, plan: {}, current: {}, max: {}, canShare: {}", 
                user.getUserId(), planType, currentCount, maxAllowed, canShare);
                
        return canShare;
    }

    /**
     * 플랜별 데이터 묶음 최대 개수 조회
     * 
     * @param user 조회할 사용자
     * @return 플랜별 최대 데이터 묶음 개수
     */
    public int getMaxDataBundleCount(User user) {
        PlanType planType = getUserPlanType(user);
        return planType.getMaxDataBundleCount();
    }

    /**
     * 사용자의 현재 플랜 제한사항 조회
     * 
     * @param user 조회할 사용자
     * @return 플랜 제한사항 정보
     */
    public PlanLimitsInfo getPlanLimits(User user) {
        PlanType planType = getUserPlanType(user);
        
        // 현재 사용량 조회
        int currentCustomApiCount = (int) customApiRepository.countByUserId(user.getUserId());
        int currentSharedApiCount = (int) sharedApiRepository.countByCreatorIdAndIsActiveTrue(user.getUserId());
        
        return PlanLimitsInfo.builder()
                .planType(planType)
                .maxCustomApiCount(planType.getMaxCustomApiCount())
                .currentCustomApiCount(currentCustomApiCount)
                .maxSharedApiCount(planType.getMaxSharedApiCount())
                .currentSharedApiCount(currentSharedApiCount)
                .maxDataBundleCount(planType.getMaxDataBundleCount())
                .maxApiCallsPerMonth(planType.getMaxApiCount())
                .rateLimitPerMinute(planType.getRateLimitPerMinute())
                .rateLimitPerHour(planType.getRateLimitPerHour())
                .rateLimitPerDay(planType.getRateLimitPerDay())
                .build();
    }

    /**
     * 플랜 업그레이드가 필요한지 확인
     * 
     * @param user 확인할 사용자
     * @param requestType 요청 타입 ("CUSTOM_API", "SHARED_API")
     * @return 업그레이드 필요 시 true
     */
    public boolean needsPlanUpgrade(User user, String requestType) {
        PlanType currentPlan = getUserPlanType(user);
        
        // 이미 PRO 플랜이면 업그레이드 불필요
        if (currentPlan == PlanType.PRO) {
            return false;
        }
        
        // FREE 플랜인 경우 제한 확인
        switch (requestType.toUpperCase()) {
            case "CUSTOM_API":
                return !canCreateCustomApi(user);
            case "SHARED_API":
                return !canShareApi(user);
            default:
                return false;
        }
    }

    /**
     * 사용자의 현재 플랜 타입 조회
     * 활성 구독이 없는 경우 FREE 플랜으로 간주
     * 
     * @param user 조회할 사용자
     * @return 사용자의 플랜 타입
     */
    private PlanType getUserPlanType(User user) {
        Optional<UserSubscription> activeSubscription = 
                userSubscriptionRepository.findActiveSubscriptionByUser(user);
        
        if (activeSubscription.isPresent()) {
            PlanType planType = activeSubscription.get().getPlan().getPlanType();
            if (planType != null) {
                return planType;
            } else {
                log.warn("플랜 타입이 null, FREE로 처리 - userId: {}", user.getUserId());
                return PlanType.FREE;
            }
        }
        
        // 활성 구독이 없는 경우 FREE 플랜
        return PlanType.FREE;
    }

    /**
     * 플랜 제한사항 정보를 담는 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class PlanLimitsInfo {
        private final PlanType planType;
        private final int maxCustomApiCount;
        private final int currentCustomApiCount;
        private final int maxSharedApiCount;
        private final int currentSharedApiCount;
        private final int maxDataBundleCount;
        private final int maxApiCallsPerMonth;
        private final int rateLimitPerMinute;
        private final int rateLimitPerHour;
        private final int rateLimitPerDay;
        
        public boolean isCustomApiLimitReached() {
            return currentCustomApiCount >= maxCustomApiCount;
        }
        
        public boolean isSharedApiLimitReached() {
            return currentSharedApiCount >= maxSharedApiCount;
        }
        
        public int getCustomApiRemaining() {
            return Math.max(0, maxCustomApiCount - currentCustomApiCount);
        }
        
        public int getSharedApiRemaining() {
            return Math.max(0, maxSharedApiCount - currentSharedApiCount);
        }
    }
}