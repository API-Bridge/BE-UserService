package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.CustomApi;
import org.example.Usersvc.domain.SharedApi;
import org.example.Usersvc.repository.CustomApiRepository;
import org.example.Usersvc.repository.SharedApiRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SharedApiService {

    private final SharedApiRepository sharedApiRepository;
    private final CustomApiRepository customApiRepository;
    private final ApiSharingValidationService apiSharingValidationService;

    @Transactional
    public SharedApi shareApi(String userId, String customApiId, String planType) {
        return shareApi(userId, customApiId, planType, null, null);
    }

    @Transactional
    public SharedApi shareApi(String userId, String customApiId, String planType, String apiName, String apiDescription) {
        log.debug("API 공유 시작 - userId: {}, customApiId: {}, planType: {}, apiName: {}, apiDescription: {}", 
                userId, customApiId, planType, apiName, apiDescription);

        CustomApi customApi = customApiRepository.findByCustomApiId(customApiId)
                .orElseThrow(() -> new IllegalArgumentException("API를 찾을 수 없습니다."));

        if (!customApi.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 API만 공유할 수 있습니다.");
        }

        if (!apiSharingValidationService.validateApiForSharing(customApi, planType)) {
            throw new IllegalArgumentException("현재 플랜에서는 이 API를 공유할 수 없습니다.");
        }

        Optional<SharedApi> existingSharedApi = sharedApiRepository.findByOriginalApiIdAndIsActiveTrue(customApiId);
        if (existingSharedApi.isPresent()) {
            throw new IllegalArgumentException("이미 공유된 API입니다.");
        }

        // 사용자가 입력한 이름과 설명을 우선 사용, 없으면 기본값 사용
        String finalApiName = (apiName != null && !apiName.trim().isEmpty()) ? apiName.trim() : customApi.getName();
        String finalApiDescription = (apiDescription != null && !apiDescription.trim().isEmpty()) ? apiDescription.trim() : customApi.getDescription();

        SharedApi sharedApi = SharedApi.builder()
                .sharedApiId(UUID.randomUUID().toString())
                .originalApiId(customApiId)
                .creatorId(userId)
                .apiName(finalApiName)
                .description(finalApiDescription)
                .dataCount(0) // 새 스키마에서는 SharedApi가 dataCount를 관리
                .build();

        SharedApi savedSharedApi = sharedApiRepository.save(sharedApi);
        log.info("API 공유 완료 - sharedApiId: {}, originalApiId: {}, finalApiName: {}, finalApiDescription: {}", 
                savedSharedApi.getSharedApiId(), customApiId, finalApiName, finalApiDescription);

        return savedSharedApi;
    }

    @Transactional
    public void unshareApi(String userId, String customApiId) {
        log.debug("API 공유 취소 시작 - userId: {}, customApiId: {}", userId, customApiId);

        SharedApi sharedApi = sharedApiRepository.findByOriginalApiIdAndIsActiveTrue(customApiId)
                .orElseThrow(() -> new IllegalArgumentException("공유되지 않은 API입니다."));

        if (!sharedApi.isCreatedBy(userId)) {
            throw new IllegalArgumentException("본인이 공유한 API만 취소할 수 있습니다.");
        }

        sharedApi.deactivate();
        sharedApiRepository.save(sharedApi);

        log.info("API 공유 취소 완료 - sharedApiId: {}, originalApiId: {}", sharedApi.getSharedApiId(), customApiId);
    }

    public List<SharedApi> getActiveSharedApis() {
        return sharedApiRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }

    public Page<SharedApi> getActiveSharedApis(Pageable pageable) {
        return sharedApiRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable);
    }

    public List<SharedApi> getSharedApisByCreator(String creatorId) {
        return sharedApiRepository.findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(creatorId);
    }

    public List<SharedApi> searchSharedApis(String keyword) {
        return sharedApiRepository.findByApiNameContainingIgnoreCaseAndIsActiveTrueOrderByCreatedAtDesc(keyword);
    }

    public List<SharedApi> getSharedApisByDataCountLimit(int maxDataCount) {
        return sharedApiRepository.findByDataCountLessThanEqualAndIsActiveTrueOrderByCreatedAtDesc(maxDataCount);
    }

    public Optional<SharedApi> getSharedApiById(String sharedApiId) {
        return sharedApiRepository.findById(sharedApiId);
    }

    public Optional<SharedApi> getSharedApiByOriginalApiId(String originalApiId) {
        return sharedApiRepository.findByOriginalApiIdAndIsActiveTrue(originalApiId);
    }
}