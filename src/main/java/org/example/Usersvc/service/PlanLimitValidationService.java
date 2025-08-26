package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.PlanName;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
// import org.example.Usersvc.repository.CustomApiRepository; // Custom API Service로 이관
// import org.example.Usersvc.repository.SharedApiRepository; // 공유 기능 비활성화
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
    // private final CustomApiRepository customApiRepository; // Custom API Service로 이관
    // private final SharedApiRepository sharedApiRepository; // 공유 기능 비활성화

    /**
     * 사용자가 새로운 Custom API를 생성할 수 있는지 검증
     * 
     * @param user 검증할 사용자
     * @return 생성 가능하면 true, 제한 초과 시 false
     */
    public boolean canCreateCustomApi(User user) {
        // Custom API 기능이 Custom API Service로 이관됨
        // 현재는 항상 true 반환 (제한 검증은 Custom API Service에서 수행)
        log.debug("Custom API 생성 제한 검증 - userId: {} (Custom API Service로 위임)", user.getUserId());
        return true;
    }

    /**
     * 사용자가 새로운 API를 공유할 수 있는지 검증
     * 
     * @param user 검증할 사용자
     * @return 공유 가능하면 true, 제한 초과 시 false
     */
    public boolean canShareApi(User user) {
        PlanName planName = getUserPlanName(user);
        // int currentCount = (int) sharedApiRepository.countByCreatorIdAndIsActiveTrue(user.getUserId()); // 공유 기능 비활성화
        int currentCount = 0; // 공유 기능 비활성화로 인한 기본값
        int maxAllowed = planName.getMaxSharedApiCount();
        
        boolean canShare = currentCount < maxAllowed;
        
        log.debug("API 공유 가능 여부 - userId: {}, plan: {}, current: {}, max: {}, canShare: {}", 
                user.getUserId(), planName, currentCount, maxAllowed, canShare);
                
        return canShare;
    }

    /**
     * 플랜별 데이터 묶음 최대 개수 조회
     * 
     * @param user 조회할 사용자
     * @return 플랜별 최대 데이터 묶음 개수
     */
    public int getMaxDataBundleCount(User user) {
        PlanName planName = getUserPlanName(user);
        return planName.getMaxDataBundleCount();
    }

    /**
     * 사용자의 현재 플랜 제한사항 조회
     * 
     * @param user 조회할 사용자
     * @return 플랜 제한사항 정보
     */
    public PlanLimitsInfo getPlanLimits(User user) {
        PlanName planName = getUserPlanName(user);
        
        // 현재 사용량 조회
        // int currentCustomApiCount = (int) customApiRepository.countByUserId(user.getUserId()); // Custom API Service로 이관
        int currentCustomApiCount = 0; // Custom API Service로 이관됨
        // int currentSharedApiCount = (int) sharedApiRepository.countByCreatorIdAndIsActiveTrue(user.getUserId()); // 공유 기능 비활성화
        int currentSharedApiCount = 0; // 공유 기능 비활성화로 인한 기본값
        
        return PlanLimitsInfo.builder()
                .planName(planName)
                .maxCustomApiCount(planName.getMaxCustomApiCount())
                .currentCustomApiCount(currentCustomApiCount)
                .maxSharedApiCount(planName.getMaxSharedApiCount())
                .currentSharedApiCount(currentSharedApiCount)
                .maxDataBundleCount(planName.getMaxDataBundleCount())
                .maxApiCallsPerMonth(planName.getMaxApiCount())
                .rateLimitPerMinute(planName.getRateLimitPerMinute())
                .rateLimitPerHour(planName.getRateLimitPerHour())
                .rateLimitPerDay(planName.getRateLimitPerDay())
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
        PlanName currentPlan = getUserPlanName(user);
        
        // 이미 PRO 플랜이면 업그레이드 불필요
        if (currentPlan == PlanName.PRO) {
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
    private PlanName getUserPlanName(User user) {
        Optional<UserSubscription> activeSubscription = 
                userSubscriptionRepository.findActiveSubscriptionByUser(user);
        
        if (activeSubscription.isPresent()) {
            PlanName planName = activeSubscription.get().getPlan().getPlanName();
            if (planName != null) {
                return planName;
            } else {
                log.warn("플랜 타입이 null, FREE로 처리 - userId: {}", user.getUserId());
                return PlanName.FREE;
            }
        }
        
        // 활성 구독이 없는 경우 FREE 플랜
        return PlanName.FREE;
    }

    /**
     * 플랜 제한사항 정보를 담는 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class PlanLimitsInfo {
        private final PlanName planName;
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