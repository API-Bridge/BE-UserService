/* 공유 기능 비활성화 - 전체 파일 주석 처리
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

    public boolean validateApiForSharing(CustomApi api, PlanName planName, String userId) {
        if (api == null || !api.isValid()) {
            log.warn("유효하지 않은 API 공유 시도 - apiId: {}", api != null ? api.getCustomApiId() : "null");
            return false;
        }

        int currentSharedCount = (int) sharedApiRepository.countByCreatorIdAndIsActiveTrue(userId);
        int maxAllowed = planName.getMaxSharedApiCount();

        log.debug("공유 API 제한 확인 - userId: {}, 현재: {}, 최대: {}", userId, currentSharedCount, maxAllowed);

        if (currentSharedCount >= maxAllowed) {
            log.warn("공유 API 개수 제한 초과 - userId: {}, 현재: {}, 최대: {}", 
                userId, currentSharedCount, maxAllowed);
            throw new PlanLimitExceededException(
                String.format("공유 API 개수 제한을 초과했습니다. (현재: %d, 최대: %d)", 
                    currentSharedCount, maxAllowed));
        }

        return true;
    }

    public boolean validateApiForSharing(CustomApi api, PlanName planName) {
        if (api == null || !api.isValid()) {
            return false;
        }

        return true;
    }

    public int getRemainingSharedApiCount(PlanName planName, String userId) {
        int currentCount = (int) sharedApiRepository.countByCreatorIdAndIsActiveTrue(userId);
        int maxAllowed = planName.getMaxSharedApiCount();
        return Math.max(0, maxAllowed - currentCount);
    }

    public boolean isSharedApiLimitExceeded(PlanName planName, String userId) {
        int currentCount = (int) sharedApiRepository.countByCreatorIdAndIsActiveTrue(userId);
        int maxAllowed = planName.getMaxSharedApiCount();
        return currentCount >= maxAllowed;
    }
}
*/