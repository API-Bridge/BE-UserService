package org.example.Usersvc.repository;

import org.example.Usersvc.domain.SharedApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("공유 API 리포지토리 테스트")
class SharedApiRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private SharedApiRepository sharedApiRepository;

    @Test
    @DisplayName("공유 API 저장 및 조회 테스트")
    void saveAndFindSharedApi() {
        SharedApi sharedApi = SharedApi.builder()
                .sharedApiId("shared-12345")
                .originalApiId("api-12345")
                .creatorId("user-12345")
                .apiName("Shared Test API")
                .description("공유된 테스트용 API")
                .dataCount(3)
                .build();

        SharedApi savedApi = sharedApiRepository.save(sharedApi);

        assertThat(savedApi.getSharedApiId()).isEqualTo("shared-12345");
        assertThat(savedApi.getOriginalApiId()).isEqualTo("api-12345");
        assertThat(savedApi.getCreatorId()).isEqualTo("user-12345");
        assertThat(savedApi.getApiName()).isEqualTo("Shared Test API");
        assertThat(savedApi.getDescription()).isEqualTo("공유된 테스트용 API");
        assertThat(savedApi.getDataCount()).isEqualTo(3);
        assertThat(savedApi.isActive()).isTrue();
        assertThat(savedApi.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("활성화된 공유 API 목록 조회 테스트")
    void findActiveSharedApis() {
        SharedApi activeApi1 = SharedApi.builder()
                .sharedApiId("shared-1")
                .originalApiId("api-1")
                .creatorId("user-1")
                .apiName("Active API 1")
                .description("활성 API 1")
                .dataCount(2)
                .build();

        SharedApi activeApi2 = SharedApi.builder()
                .sharedApiId("shared-2")
                .originalApiId("api-2")
                .creatorId("user-2")
                .apiName("Active API 2")
                .description("활성 API 2")
                .dataCount(5)
                .build();

        SharedApi inactiveApi = SharedApi.builder()
                .sharedApiId("shared-3")
                .originalApiId("api-3")
                .creatorId("user-3")
                .apiName("Inactive API")
                .description("비활성 API")
                .dataCount(1)
                .build();
        inactiveApi.deactivate();

        sharedApiRepository.save(activeApi1);
        sharedApiRepository.save(activeApi2);
        sharedApiRepository.save(inactiveApi);

        List<SharedApi> activeApis = sharedApiRepository.findByIsActiveTrueOrderByCreatedAtDesc();

        assertThat(activeApis).hasSize(2);
        assertThat(activeApis).extracting(SharedApi::getSharedApiId).containsExactlyInAnyOrder("shared-1", "shared-2");
    }

    @Test
    @DisplayName("생성자별 공유 API 조회 테스트")
    void findByCreatorId() {
        String creatorId = "user-12345";

        SharedApi api1 = SharedApi.builder()
                .sharedApiId("shared-1")
                .originalApiId("api-1")
                .creatorId(creatorId)
                .apiName("Creator API 1")
                .description("생성자 API 1")
                .dataCount(3)
                .build();

        SharedApi api2 = SharedApi.builder()
                .sharedApiId("shared-2")
                .originalApiId("api-2")
                .creatorId(creatorId)
                .apiName("Creator API 2")
                .description("생성자 API 2")
                .dataCount(5)
                .build();

        SharedApi otherUserApi = SharedApi.builder()
                .sharedApiId("shared-3")
                .originalApiId("api-3")
                .creatorId("user-67890")
                .apiName("Other User API")
                .description("다른 사용자 API")
                .dataCount(2)
                .build();

        sharedApiRepository.save(api1);
        sharedApiRepository.save(api2);
        sharedApiRepository.save(otherUserApi);

        List<SharedApi> creatorApis = sharedApiRepository.findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(creatorId);

        assertThat(creatorApis).hasSize(2);
        assertThat(creatorApis).extracting(SharedApi::getSharedApiId).containsExactlyInAnyOrder("shared-1", "shared-2");
    }

    @Test
    @DisplayName("원본 API ID로 공유 API 조회 테스트")
    void findByOriginalApiId() {
        String originalApiId = "api-12345";

        SharedApi sharedApi = SharedApi.builder()
                .sharedApiId("shared-12345")
                .originalApiId(originalApiId)
                .creatorId("user-12345")
                .apiName("Shared API")
                .description("공유 API")
                .dataCount(3)
                .build();

        sharedApiRepository.save(sharedApi);

        Optional<SharedApi> foundApi = sharedApiRepository.findByOriginalApiIdAndIsActiveTrue(originalApiId);

        assertThat(foundApi).isPresent();
        assertThat(foundApi.get().getOriginalApiId()).isEqualTo(originalApiId);
    }

    @Test
    @DisplayName("비활성화된 공유 API는 조회되지 않는 테스트")
    void inactiveApiNotFound() {
        SharedApi sharedApi = SharedApi.builder()
                .sharedApiId("shared-12345")
                .originalApiId("api-12345")
                .creatorId("user-12345")
                .apiName("Inactive API")
                .description("비활성 API")
                .dataCount(3)
                .build();
        sharedApi.deactivate();

        sharedApiRepository.save(sharedApi);

        Optional<SharedApi> foundApi = sharedApiRepository.findByOriginalApiIdAndIsActiveTrue("api-12345");

        assertThat(foundApi).isEmpty();
    }

    @Test
    @DisplayName("키워드로 공유 API 검색 테스트")
    void searchByKeyword() {
        SharedApi api1 = SharedApi.builder()
                .sharedApiId("shared-1")
                .originalApiId("api-1")
                .creatorId("user-1")
                .apiName("Weather API")
                .description("날씨 정보를 제공하는 API")
                .dataCount(5)
                .build();

        SharedApi api2 = SharedApi.builder()
                .sharedApiId("shared-2")
                .originalApiId("api-2")
                .creatorId("user-2")
                .apiName("News API")
                .description("뉴스 정보를 제공하는 API")
                .dataCount(3)
                .build();

        SharedApi api3 = SharedApi.builder()
                .sharedApiId("shared-3")
                .originalApiId("api-3")
                .creatorId("user-3")
                .apiName("Weather Forecast")
                .description("일기예보 서비스")
                .dataCount(7)
                .build();

        sharedApiRepository.save(api1);
        sharedApiRepository.save(api2);
        sharedApiRepository.save(api3);

        List<SharedApi> weatherApis = sharedApiRepository.findByApiNameContainingIgnoreCaseAndIsActiveTrueOrderByCreatedAtDesc("weather");

        assertThat(weatherApis).hasSize(2);
        assertThat(weatherApis).extracting(SharedApi::getSharedApiId).containsExactlyInAnyOrder("shared-1", "shared-3");
    }

    @Test
    @DisplayName("페이징된 공유 API 조회 테스트")
    void findActiveSharedApisWithPagination() {
        for (int i = 1; i <= 7; i++) {
            SharedApi api = SharedApi.builder()
                    .sharedApiId("shared-" + i)
                    .originalApiId("api-" + i)
                    .creatorId("user-" + i)
                    .apiName("API " + i)
                    .description("API " + i + " 설명")
                    .dataCount(i)
                    .build();
            sharedApiRepository.save(api);
        }

        Pageable pageable = PageRequest.of(0, 5);
        Page<SharedApi> page = sharedApiRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable);

        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(7);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("데이터 수로 필터링된 공유 API 조회 테스트")
    void findByDataCountFilter() {
        SharedApi smallApi = SharedApi.builder()
                .sharedApiId("shared-small")
                .originalApiId("api-small")
                .creatorId("user-1")
                .apiName("Small API")
                .description("작은 데이터 API")
                .dataCount(2)
                .build();

        SharedApi largeApi = SharedApi.builder()
                .sharedApiId("shared-large")
                .originalApiId("api-large")
                .creatorId("user-2")
                .apiName("Large API")
                .description("큰 데이터 API")
                .dataCount(15)
                .build();

        sharedApiRepository.save(smallApi);
        sharedApiRepository.save(largeApi);

        List<SharedApi> smallApis = sharedApiRepository.findByDataCountLessThanEqualAndIsActiveTrueOrderByCreatedAtDesc(5);

        assertThat(smallApis).hasSize(1);
        assertThat(smallApis.get(0).getSharedApiId()).isEqualTo("shared-small");
    }
}