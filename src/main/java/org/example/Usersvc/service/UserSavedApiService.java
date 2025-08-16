package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.SharedApi;
import org.example.Usersvc.domain.UserSavedApi;
import org.example.Usersvc.repository.SharedApiRepository;
import org.example.Usersvc.repository.UserSavedApiRepository;
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
public class UserSavedApiService {

    private final UserSavedApiRepository userSavedApiRepository;
    private final SharedApiRepository sharedApiRepository;

    @Transactional
    public UserSavedApi saveSharedApi(String userId, String sharedApiId) {
        log.debug("공유 API 저장 시작 - userId: {}, sharedApiId: {}", userId, sharedApiId);

        SharedApi sharedApi = sharedApiRepository.findById(sharedApiId)
                .orElseThrow(() -> new IllegalArgumentException("공유 API를 찾을 수 없습니다."));

        if (!sharedApi.isActive()) {
            throw new IllegalArgumentException("비활성화된 공유 API입니다.");
        }

        Optional<UserSavedApi> existingSavedApi = userSavedApiRepository
                .findByUserIdAndSharedApiIdAndDeletedFalse(userId, sharedApiId);
        if (existingSavedApi.isPresent()) {
            throw new IllegalArgumentException("이미 저장된 API입니다.");
        }

        UserSavedApi userSavedApi = UserSavedApi.builder()
                .userApiId(UUID.randomUUID().toString())
                .userId(userId)
                .sharedApiId(sharedApiId)
                .apiName(sharedApi.getApiName())
                .description(sharedApi.getDescription())
                .dataCount(sharedApi.getDataCount())
                .build();

        UserSavedApi savedApi = userSavedApiRepository.save(userSavedApi);
        log.info("공유 API 저장 완료 - userApiId: {}, sharedApiId: {}", savedApi.getUserApiId(), sharedApiId);

        return savedApi;
    }

    @Transactional
    public void deleteSavedApi(String userId, String userApiId) {
        log.debug("저장된 API 삭제 시작 - userId: {}, userApiId: {}", userId, userApiId);

        UserSavedApi savedApi = userSavedApiRepository.findByUserApiIdAndDeletedFalse(userApiId)
                .orElseThrow(() -> new IllegalArgumentException("저장된 API를 찾을 수 없습니다."));

        if (!savedApi.isOwnedBy(userId)) {
            throw new IllegalArgumentException("본인이 저장한 API만 삭제할 수 있습니다.");
        }

        savedApi.markAsDeleted();
        userSavedApiRepository.save(savedApi);

        log.info("저장된 API 삭제 완료 - userApiId: {}", userApiId);
    }

    public List<UserSavedApi> getUserSavedApis(String userId) {
        return userSavedApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);
    }

    public Page<UserSavedApi> getUserSavedApis(String userId, Pageable pageable) {
        return userSavedApiRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable);
    }

    public Optional<UserSavedApi> getSavedApiById(String userApiId) {
        return userSavedApiRepository.findByUserApiIdAndDeletedFalse(userApiId);
    }

    public List<UserSavedApi> getSavedApisBySharedApiId(String sharedApiId) {
        return userSavedApiRepository.findBySharedApiIdAndDeletedFalse(sharedApiId);
    }
}