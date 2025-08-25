package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.exception.PlanLimitExceededException;
import org.example.Usersvc.common.constants.BusinessConstants;
import org.example.Usersvc.domain.CustomApi;
import org.example.Usersvc.domain.PlanName;
import org.example.Usersvc.repository.SharedApiRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiSharingValidationService {

    private final SharedApiRepository sharedApiRepository;

    public int getMaxDataCountForPlan(PlanName planName) {
        switch (planName) {
            case FREE:
                return BusinessConstants.FREE_MAX_SHARED_APIS;
            case PRO:
                return BusinessConstants.PRO_MAX_SHARED_APIS;
            default:
                return 0;
        }
    }

    /**
     * API 공유 가능 여부 검증 (제한 확인 포함)
     * 
     * @param api 공유할 API
     * @param planName 사용자 플랜 타입
     * @param userId 사용자 ID
     * @return 공유 가능하면 true
     * @throws PlanLimitExceededException 제한 초과 시
     */
    public boolean validateApiForSharing(CustomApi api, PlanName planName, String userId) {
        if (api == null || !api.isValid()) {
            log.warn("유효하지 않은 API 공유 시도 - apiId: {}", api != null ? api.getCustomApiId() : "null");
            return false;
        }

        // 현재 공유 API 개수 확인
        int currentSharedCount = (int) sharedApiRepository.countByCreatorIdAndIsActiveTrue(userId);
        int maxAllowed = planName.getMaxSharedApiCount();

        log.debug("공유 API 제한 확인 - userId: {}, 현재: {}, 최대: {}", userId, currentSharedCount, maxAllowed);

        if (currentSharedCount >= maxAllowed) {
            throw PlanLimitExceededException.withUpgradeRecommendation(
                String.format("공유 API 제한을 초과했습니다. 현재: %d개, 최대: %d개.", currentSharedCount, maxAllowed),
                "SharedAPI", currentSharedCount, maxAllowed
            );
        }

        return true;
    }

    /**
     * 기존 호환성을 위한 메서드 (deprecated)
     * 
     * @deprecated validateApiForSharing(CustomApi, PlanName, String) 사용 권장
     */
    @Deprecated
    public boolean validateApiForSharing(CustomApi api, PlanName planName) {
        if (api == null) {
            return false;
        }
        // 기존 로직 유지 - 제한 검사 없이 유효성만 확인
        return api.isValid();
    }

    /**
     * 사용자의 공유 API 생성 가능 개수 조회
     * 
     * @param planName 플랜 타입
     * @param userId 사용자 ID
     * @return 생성 가능한 공유 API 개수
     */
    public int getRemainingSharedApiCount(PlanName planName, String userId) {
        int currentCount = (int) sharedApiRepository.countByCreatorIdAndIsActiveTrue(userId);
        int maxAllowed = planName.getMaxSharedApiCount();
        return Math.max(0, maxAllowed - currentCount);
    }

    /**
     * 공유 API 제한 초과 여부 확인
     * 
     * @param planName 플랜 타입
     * @param userId 사용자 ID
     * @return 제한 초과 시 true
     */
    public boolean isSharedApiLimitExceeded(PlanName planName, String userId) {
        int currentCount = (int) sharedApiRepository.countByCreatorIdAndIsActiveTrue(userId);
        int maxAllowed = planName.getMaxSharedApiCount();
        return currentCount >= maxAllowed;
    }
}