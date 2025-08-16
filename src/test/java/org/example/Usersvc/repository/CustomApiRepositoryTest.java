package org.example.Usersvc.repository;

import org.example.Usersvc.domain.CustomApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("커스텀 API 리포지토리 테스트")
class CustomApiRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CustomApiRepository customApiRepository;

    @Test
    @DisplayName("커스텀 API 저장 및 조회 테스트")
    void saveAndFindCustomApi() {
        CustomApi customApi = CustomApi.builder()
                .apiId("api-12345")
                .userId("user-12345")
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(3)
                .build();

        CustomApi savedApi = customApiRepository.save(customApi);

        assertThat(savedApi.getApiId()).isEqualTo("api-12345");
        assertThat(savedApi.getUserId()).isEqualTo("user-12345");
        assertThat(savedApi.getApiName()).isEqualTo("Test API");
        assertThat(savedApi.getDescription()).isEqualTo("테스트용 API");
        assertThat(savedApi.getDataCount()).isEqualTo(3);
        assertThat(savedApi.isDeleted()).isFalse();
        assertThat(savedApi.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용자별 커스텀 API 조회 테스트")
    void findByUserIdAndDeletedFalse() {
        String userId = "user-12345";

        CustomApi api1 = CustomApi.builder()
                .apiId("api-1")
                .userId(userId)
                .apiName("API 1")
                .description("첫 번째 API")
                .dataCount(2)
                .build();

        CustomApi api2 = CustomApi.builder()
                .apiId("api-2")
                .userId(userId)
                .apiName("API 2")
                .description("두 번째 API")
                .dataCount(5)
                .build();

        CustomApi api3 = CustomApi.builder()
                .apiId("api-3")
                .userId("user-67890")
                .apiName("API 3")
                .description("다른 사용자 API")
                .dataCount(1)
                .build();

        customApiRepository.save(api1);
        customApiRepository.save(api2);
        customApiRepository.save(api3);

        List<CustomApi> userApis = customApiRepository.findByUserIdAndDeletedFalse(userId);

        assertThat(userApis).hasSize(2);
        assertThat(userApis).extracting(CustomApi::getApiId).containsExactlyInAnyOrder("api-1", "api-2");
    }

    @Test
    @DisplayName("삭제되지 않은 커스텀 API 조회 테스트")
    void findActiveCustomApis() {
        CustomApi activeApi = CustomApi.builder()
                .apiId("api-active")
                .userId("user-12345")
                .apiName("Active API")
                .description("활성 API")
                .dataCount(3)
                .build();

        CustomApi deletedApi = CustomApi.builder()
                .apiId("api-deleted")
                .userId("user-12345")
                .apiName("Deleted API")
                .description("삭제된 API")
                .dataCount(2)
                .build();
        deletedApi.markAsDeleted();

        customApiRepository.save(activeApi);
        customApiRepository.save(deletedApi);

        List<CustomApi> activeApis = customApiRepository.findByUserIdAndDeletedFalse("user-12345");

        assertThat(activeApis).hasSize(1);
        assertThat(activeApis.get(0).getApiId()).isEqualTo("api-active");
    }

    @Test
    @DisplayName("커스텀 API ID로 조회 테스트")
    void findByApiIdAndDeletedFalse() {
        CustomApi customApi = CustomApi.builder()
                .apiId("api-12345")
                .userId("user-12345")
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(3)
                .build();

        customApiRepository.save(customApi);

        Optional<CustomApi> foundApi = customApiRepository.findByApiIdAndDeletedFalse("api-12345");

        assertThat(foundApi).isPresent();
        assertThat(foundApi.get().getApiId()).isEqualTo("api-12345");
    }

    @Test
    @DisplayName("삭제된 커스텀 API는 조회되지 않는 테스트")
    void deletedApiNotFound() {
        CustomApi customApi = CustomApi.builder()
                .apiId("api-12345")
                .userId("user-12345")
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(3)
                .build();
        customApi.markAsDeleted();

        customApiRepository.save(customApi);

        Optional<CustomApi> foundApi = customApiRepository.findByApiIdAndDeletedFalse("api-12345");

        assertThat(foundApi).isEmpty();
    }

    @Test
    @DisplayName("데이터 수로 필터링된 커스텀 API 조회 테스트")
    void findByDataCountLessThanEqual() {
        CustomApi api1 = CustomApi.builder()
                .apiId("api-1")
                .userId("user-12345")
                .apiName("Small API")
                .description("작은 API")
                .dataCount(2)
                .build();

        CustomApi api2 = CustomApi.builder()
                .apiId("api-2")
                .userId("user-12345")
                .apiName("Medium API")
                .description("중간 API")
                .dataCount(5)
                .build();

        CustomApi api3 = CustomApi.builder()
                .apiId("api-3")
                .userId("user-12345")
                .apiName("Large API")
                .description("큰 API")
                .dataCount(10)
                .build();

        customApiRepository.save(api1);
        customApiRepository.save(api2);
        customApiRepository.save(api3);

        List<CustomApi> smallApis = customApiRepository.findByUserIdAndDataCountLessThanEqualAndDeletedFalse("user-12345", 3);

        assertThat(smallApis).hasSize(1);
        assertThat(smallApis.get(0).getApiId()).isEqualTo("api-1");
    }

    @Test
    @DisplayName("페이징된 커스텀 API 조회 테스트")
    void findByUserIdWithPagination() {
        String userId = "user-12345";

        for (int i = 1; i <= 5; i++) {
            CustomApi api = CustomApi.builder()
                    .apiId("api-" + i)
                    .userId(userId)
                    .apiName("API " + i)
                    .description("API " + i + " 설명")
                    .dataCount(i)
                    .build();
            customApiRepository.save(api);
        }

        Pageable pageable = PageRequest.of(0, 3);
        Page<CustomApi> page = customApiRepository.findByUserIdAndDeletedFalse(userId, pageable);

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }
}