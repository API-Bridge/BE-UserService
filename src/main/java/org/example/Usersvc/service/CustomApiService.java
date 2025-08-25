package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.exception.PlanLimitExceededException;
import org.example.Usersvc.domain.CustomApi;
import org.example.Usersvc.domain.PlanName;
import org.example.Usersvc.repository.CustomApiRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CustomApiService {

    private final CustomApiRepository customApiRepository;

    public List<CustomApi> getCustomApisByUserId(String userId) {
        log.debug("사용자 커스텀 API 목록 조회 - userId: {}", userId);
        return customApiRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<CustomApi> getCustomApisByUserId(String userId, Pageable pageable) {
        log.debug("사용자 커스텀 API 목록 조회 (페이징) - userId: {}, pageable: {}", userId, pageable);
        return customApiRepository.findByUserId(userId, pageable);
    }

    public CustomApi getCustomApi(String userId, String customApiId) {
        log.debug("커스텀 API 상세 조회 - userId: {}, customApiId: {}", userId, customApiId);
        
        CustomApi customApi = customApiRepository.findByCustomApiId(customApiId)
                .orElseThrow(() -> new IllegalArgumentException("API를 찾을 수 없습니다."));

        if (!customApi.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 API만 조회할 수 있습니다.");
        }

        return customApi;
    }

    public List<CustomApi> searchCustomApis(String keyword) {
        log.debug("커스텀 API 검색 - keyword: {}", keyword);
        return customApiRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(keyword);
    }

    /**
     * 커스텀 API 생성 전 제한 검증
     * 
     * @param userId 사용자 ID
     * @param planName 사용자 플랜 타입
     * @throws PlanLimitExceededException 제한 초과 시
     */
    public void validateCustomApiCreation(String userId, PlanName planName) {
        int currentCount = (int) customApiRepository.countByUserId(userId);
        int maxAllowed = planName.getMaxCustomApiCount();

        log.debug("커스텀 API 생성 제한 확인 - userId: {}, 현재: {}, 최대: {}", userId, currentCount, maxAllowed);

        if (currentCount >= maxAllowed) {
            throw PlanLimitExceededException.withUpgradeRecommendation(
                String.format("커스텀 API 제한을 초과했습니다. 현재: %d개, 최대: %d개.", currentCount, maxAllowed),
                "CustomAPI", currentCount, maxAllowed
            );
        }
    }

    /**
     * 사용자의 커스텀 API 생성 가능 개수 조회
     * 
     * @param userId 사용자 ID
     * @param planName 플랜 타입
     * @return 생성 가능한 커스텀 API 개수
     */
    public int getRemainingCustomApiCount(String userId, PlanName planName) {
        int currentCount = (int) customApiRepository.countByUserId(userId);
        int maxAllowed = planName.getMaxCustomApiCount();
        return Math.max(0, maxAllowed - currentCount);
    }

    /**
     * 커스텀 API 제한 초과 여부 확인
     * 
     * @param userId 사용자 ID
     * @param planName 플랜 타입
     * @return 제한 초과 시 true
     */
    public boolean isCustomApiLimitExceeded(String userId, PlanName planName) {
        int currentCount = (int) customApiRepository.countByUserId(userId);
        int maxAllowed = planName.getMaxCustomApiCount();
        return currentCount >= maxAllowed;
    }
}