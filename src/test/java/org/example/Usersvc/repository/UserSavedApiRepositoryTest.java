package org.example.Usersvc.repository;

import org.example.Usersvc.domain.UserSavedApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("사용자 저장 API 리포지토리 테스트")
class UserSavedApiRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UserSavedApiRepository userSavedApiRepository;

    @Test
    @DisplayName("사용자 저장 API 저장 및 조회 테스트")
    void saveAndFindUserSavedApi() {
        UserSavedApi userSavedApi = UserSavedApi.builder()
                .userApiId("user-api-12345")
                .userId("user-12345")
                .sharedApiId("shared-12345")
                .apiName("Saved Test API")
                .description("저장된 테스트용 API")
                .dataCount(3)
                .build();

        UserSavedApi savedApi = userSavedApiRepository.save(userSavedApi);

        assertThat(savedApi.getUserApiId()).isEqualTo("user-api-12345");
        assertThat(savedApi.getUserId()).isEqualTo("user-12345");
        assertThat(savedApi.getSharedApiId()).isEqualTo("shared-12345");
        assertThat(savedApi.getApiName()).isEqualTo("Saved Test API");
        assertThat(savedApi.getDescription()).isEqualTo("저장된 테스트용 API");
        assertThat(savedApi.getDataCount()).isEqualTo(3);
        assertThat(savedApi.isDeleted()).isFalse();
        assertThat(savedApi.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용자별 저장된 API 조회 테스트")
    void findByUserIdAndDeletedFalse() {
        String userId = "user-12345";

        UserSavedApi api1 = UserSavedApi.builder()
                .userApiId("user-api-1")
                .userId(userId)
                .sharedApiId("shared-1")
                .apiName("Saved API 1")
                .description("저장된 API 1")
                .dataCount(2)
                .build();

        UserSavedApi api2 = UserSavedApi.builder()
                .userApiId("user-api-2")
                .userId(userId)
                .sharedApiId("shared-2")
                .apiName("Saved API 2")
                .description("저장된 API 2")
                .dataCount(5)
                .build();

        UserSavedApi otherUserApi = UserSavedApi.builder()
                .userApiId("user-api-3")
                .userId("user-67890")
                .sharedApiId("shared-3")
                .apiName("Other User API")
                .description("다른 사용자 API")
                .dataCount(1)
                .build();

        userSavedApiRepository.save(api1);
        userSavedApiRepository.save(api2);
        userSavedApiRepository.save(otherUserApi);

        List<UserSavedApi> userApis = userSavedApiRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        assertThat(userApis).hasSize(2);
        assertThat(userApis).extracting(UserSavedApi::getUserApiId).containsExactlyInAnyOrder("user-api-1", "user-api-2");
    }

    @Test
    @DisplayName("삭제되지 않은 사용자 저장 API 조회 테스트")
    void findActiveSavedApis() {
        UserSavedApi activeApi = UserSavedApi.builder()
                .userApiId("user-api-active")
                .userId("user-12345")
                .sharedApiId("shared-active")
                .apiName("Active Saved API")
                .description("활성 저장 API")
                .dataCount(3)
                .build();

        UserSavedApi deletedApi = UserSavedApi.builder()
                .userApiId("user-api-deleted")
                .userId("user-12345")
                .sharedApiId("shared-deleted")
                .apiName("Deleted Saved API")
                .description("삭제된 저장 API")
                .dataCount(2)
                .build();
        deletedApi.markAsDeleted();

        userSavedApiRepository.save(activeApi);
        userSavedApiRepository.save(deletedApi);

        List<UserSavedApi> activeApis = userSavedApiRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc("user-12345");

        assertThat(activeApis).hasSize(1);
        assertThat(activeApis.get(0).getUserApiId()).isEqualTo("user-api-active");
    }

    @Test
    @DisplayName("사용자 API ID로 조회 테스트")
    void findByUserApiIdAndIsDeletedFalse() {
        UserSavedApi userSavedApi = UserSavedApi.builder()
                .userApiId("user-api-12345")
                .userId("user-12345")
                .sharedApiId("shared-12345")
                .apiName("Saved Test API")
                .description("저장된 테스트용 API")
                .dataCount(3)
                .build();

        userSavedApiRepository.save(userSavedApi);

        Optional<UserSavedApi> foundApi = userSavedApiRepository.findByUserApiIdAndIsDeletedFalse("user-api-12345");

        assertThat(foundApi).isPresent();
        assertThat(foundApi.get().getUserApiId()).isEqualTo("user-api-12345");
    }

    @Test
    @DisplayName("공유 API 기반 사용자 저장 API 조회 테스트")
    void findBySharedApiId() {
        String sharedApiId = "shared-12345";

        UserSavedApi savedApi1 = UserSavedApi.builder()
                .userApiId("user-api-1")
                .userId("user-1")
                .sharedApiId(sharedApiId)
                .apiName("User 1 Saved API")
                .description("사용자 1 저장 API")
                .dataCount(3)
                .build();

        UserSavedApi savedApi2 = UserSavedApi.builder()
                .userApiId("user-api-2")
                .userId("user-2")
                .sharedApiId(sharedApiId)
                .apiName("User 2 Saved API")
                .description("사용자 2 저장 API")
                .dataCount(3)
                .build();

        UserSavedApi otherSharedApi = UserSavedApi.builder()
                .userApiId("user-api-3")
                .userId("user-3")
                .sharedApiId("shared-67890")
                .apiName("Other Shared API")
                .description("다른 공유 API")
                .dataCount(2)
                .build();

        userSavedApiRepository.save(savedApi1);
        userSavedApiRepository.save(savedApi2);
        userSavedApiRepository.save(otherSharedApi);

        List<UserSavedApi> basedOnSharedApis = userSavedApiRepository.findBySharedApiIdAndIsDeletedFalse(sharedApiId);

        assertThat(basedOnSharedApis).hasSize(2);
        assertThat(basedOnSharedApis).extracting(UserSavedApi::getUserApiId).containsExactlyInAnyOrder("user-api-1", "user-api-2");
    }

    @Test
    @DisplayName("삭제된 사용자 저장 API는 조회되지 않는 테스트")
    void deletedSavedApiNotFound() {
        UserSavedApi userSavedApi = UserSavedApi.builder()
                .userApiId("user-api-12345")
                .userId("user-12345")
                .sharedApiId("shared-12345")
                .apiName("Deleted Saved API")
                .description("삭제된 저장 API")
                .dataCount(3)
                .build();
        userSavedApi.markAsDeleted();

        userSavedApiRepository.save(userSavedApi);

        Optional<UserSavedApi> foundApi = userSavedApiRepository.findByUserApiIdAndIsDeletedFalse("user-api-12345");

        assertThat(foundApi).isEmpty();
    }

    @Test
    @DisplayName("사용자와 공유 API ID로 중복 확인 테스트")
    void checkDuplicateSave() {
        String userId = "user-12345";
        String sharedApiId = "shared-12345";

        UserSavedApi existingApi = UserSavedApi.builder()
                .userApiId("user-api-12345")
                .userId(userId)
                .sharedApiId(sharedApiId)
                .apiName("Existing Saved API")
                .description("기존 저장 API")
                .dataCount(3)
                .build();

        userSavedApiRepository.save(existingApi);

        Optional<UserSavedApi> duplicate = userSavedApiRepository.findByUserIdAndSharedApiIdAndIsDeletedFalse(userId, sharedApiId);

        assertThat(duplicate).isPresent();
        assertThat(duplicate.get().getUserId()).isEqualTo(userId);
        assertThat(duplicate.get().getSharedApiId()).isEqualTo(sharedApiId);
    }

    @Test
    @DisplayName("페이징된 사용자 저장 API 조회 테스트")
    void findUserSavedApisWithPagination() {
        String userId = "user-12345";

        for (int i = 1; i <= 7; i++) {
            UserSavedApi api = UserSavedApi.builder()
                    .userApiId("user-api-" + i)
                    .userId(userId)
                    .sharedApiId("shared-" + i)
                    .apiName("Saved API " + i)
                    .description("저장된 API " + i)
                    .dataCount(i)
                    .build();
            userSavedApiRepository.save(api);
        }

        Pageable pageable = PageRequest.of(0, 5);
        Page<UserSavedApi> page = userSavedApiRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);

        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(7);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }
}