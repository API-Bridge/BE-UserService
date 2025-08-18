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
                .customApiId("api-12345")
                .userId("user-12345")
                .name("Test API")
                .description("테스트용 API")
                .build();

        CustomApi savedApi = customApiRepository.save(customApi);

        assertThat(savedApi.getCustomApiId()).isEqualTo("api-12345");
        assertThat(savedApi.getUserId()).isEqualTo("user-12345");
        assertThat(savedApi.getName()).isEqualTo("Test API");
        assertThat(savedApi.getDescription()).isEqualTo("테스트용 API");
        assertThat(savedApi.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용자별 커스텀 API 조회 테스트")
    void findByUserId() {
        String userId = "user-12345";

        CustomApi api1 = CustomApi.builder()
                .customApiId("api-1")
                .userId(userId)
                .name("API 1")
                .description("첫 번째 API")
                .build();

        CustomApi api2 = CustomApi.builder()
                .customApiId("api-2")
                .userId(userId)
                .name("API 2")
                .description("두 번째 API")
                .build();

        CustomApi api3 = CustomApi.builder()
                .customApiId("api-3")
                .userId("user-67890")
                .name("API 3")
                .description("다른 사용자 API")
                .build();

        customApiRepository.save(api1);
        customApiRepository.save(api2);
        customApiRepository.save(api3);

        List<CustomApi> userApis = customApiRepository.findByUserId(userId);

        assertThat(userApis).hasSize(2);
        assertThat(userApis).extracting(CustomApi::getCustomApiId).containsExactlyInAnyOrder("api-1", "api-2");
    }

    @Test
    @DisplayName("커스텀 API 조회 테스트")
    void findActiveCustomApis() {
        CustomApi activeApi = CustomApi.builder()
                .customApiId("api-active")
                .userId("user-12345")
                .name("Active API")
                .description("활성 API")
                .build();

        customApiRepository.save(activeApi);

        List<CustomApi> userApis = customApiRepository.findByUserId("user-12345");

        assertThat(userApis).hasSize(1);
        assertThat(userApis.get(0).getCustomApiId()).isEqualTo("api-active");
    }

    @Test
    @DisplayName("커스텀 API ID로 조회 테스트")
    void findByCustomApiId() {
        CustomApi customApi = CustomApi.builder()
                .customApiId("api-12345")
                .userId("user-12345")
                .name("Test API")
                .description("테스트용 API")
                .build();

        customApiRepository.save(customApi);

        Optional<CustomApi> foundApi = customApiRepository.findByCustomApiId("api-12345");

        assertThat(foundApi).isPresent();
        assertThat(foundApi.get().getCustomApiId()).isEqualTo("api-12345");
    }

    @Test
    @DisplayName("빈 커스텀 API ID로 조회 테스트")
    void notFoundApiById() {
        Optional<CustomApi> foundApi = customApiRepository.findByCustomApiId("non-existent-api");

        assertThat(foundApi).isEmpty();
    }

    @Test
    @DisplayName("이름으로 커스텀 API 검색 테스트")
    void findByNameContainingIgnoreCase() {
        CustomApi api1 = CustomApi.builder()
                .customApiId("api-1")
                .userId("user-12345")
                .name("Weather API")
                .description("날씨 API")
                .build();

        CustomApi api2 = CustomApi.builder()
                .customApiId("api-2")
                .userId("user-12345")
                .name("News API")
                .description("뉴스 API")
                .build();

        CustomApi api3 = CustomApi.builder()
                .customApiId("api-3")
                .userId("user-12345")
                .name("Weather Forecast")
                .description("날씨 예보 API")
                .build();

        customApiRepository.save(api1);
        customApiRepository.save(api2);
        customApiRepository.save(api3);

        List<CustomApi> weatherApis = customApiRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc("weather");

        assertThat(weatherApis).hasSize(2);
        assertThat(weatherApis).extracting(CustomApi::getCustomApiId).containsExactlyInAnyOrder("api-1", "api-3");
    }

    @Test
    @DisplayName("페이징된 커스텀 API 조회 테스트")
    void findByUserIdWithPagination() {
        String userId = "user-12345";

        for (int i = 1; i <= 5; i++) {
            CustomApi api = CustomApi.builder()
                    .customApiId("api-" + i)
                    .userId(userId)
                    .name("API " + i)
                    .description("API " + i + " 설명")
                    .build();
            customApiRepository.save(api);
        }

        Pageable pageable = PageRequest.of(0, 3);
        Page<CustomApi> page = customApiRepository.findByUserId(userId, pageable);

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }
}