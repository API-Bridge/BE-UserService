package org.example.Usersvc.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

@DisplayName("커스텀 API 도메인 테스트")
class CustomApiTest {

    @Test
    @DisplayName("커스텀 API 생성 테스트")
    void createCustomApi() {
        String apiId = "api-12345";
        String userId = "user-12345";
        String apiName = "Test API";
        String description = "테스트용 API";
        int dataCount = 3;

        CustomApi customApi = CustomApi.builder()
                .apiId(apiId)
                .userId(userId)
                .apiName(apiName)
                .description(description)
                .dataCount(dataCount)
                .build();

        assertThat(customApi.getApiId()).isEqualTo(apiId);
        assertThat(customApi.getUserId()).isEqualTo(userId);
        assertThat(customApi.getApiName()).isEqualTo(apiName);
        assertThat(customApi.getDescription()).isEqualTo(description);
        assertThat(customApi.getDataCount()).isEqualTo(dataCount);
        assertThat(customApi.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("커스텀 API 정보 업데이트 테스트")
    void updateCustomApiInfo() {
        CustomApi customApi = CustomApi.builder()
                .apiId("api-12345")
                .userId("user-12345")
                .apiName("Original API")
                .description("원본 설명")
                .dataCount(3)
                .build();

        customApi.updateApiInfo("Updated API", "업데이트된 설명");

        assertThat(customApi.getApiName()).isEqualTo("Updated API");
        assertThat(customApi.getDescription()).isEqualTo("업데이트된 설명");
    }

    @Test
    @DisplayName("커스텀 API 데이터 수 업데이트 테스트")
    void updateDataCount() {
        CustomApi customApi = CustomApi.builder()
                .apiId("api-12345")
                .userId("user-12345")
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(3)
                .build();

        customApi.updateDataCount(5);

        assertThat(customApi.getDataCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("커스텀 API 삭제(Soft Delete) 테스트")
    void softDeleteCustomApi() {
        CustomApi customApi = CustomApi.builder()
                .apiId("api-12345")
                .userId("user-12345")
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(3)
                .build();

        customApi.markAsDeleted();

        assertThat(customApi.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("유효한 커스텀 API 검증 테스트")
    void isValidCustomApi() {
        CustomApi validApi = CustomApi.builder()
                .apiId("api-12345")
                .userId("user-12345")
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(3)
                .build();
        validApi.setCreatedAt(LocalDateTime.now());

        assertThat(validApi.isValid()).isTrue();
    }

    @Test
    @DisplayName("무효한 커스텀 API 검증 테스트")
    void isInvalidCustomApi() {
        CustomApi invalidApi = CustomApi.builder()
                .apiId(null)
                .userId("user-12345")
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(3)
                .build();

        assertThat(invalidApi.isValid()).isFalse();
    }

    @Test
    @DisplayName("플랜별 공유 가능 여부 검증 - 무료 플랜")
    void canShareForFreePlan() {
        CustomApi customApi = CustomApi.builder()
                .apiId("api-12345")
                .userId("user-12345")
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(3)
                .build();

        assertThat(customApi.canShareForPlan("FREE")).isTrue();
    }

    @Test
    @DisplayName("플랜별 공유 불가능 검증 - 무료 플랜")
    void cannotShareForFreePlan() {
        CustomApi customApi = CustomApi.builder()
                .apiId("api-12345")
                .userId("user-12345")
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(5)
                .build();

        assertThat(customApi.canShareForPlan("FREE")).isFalse();
    }

    @Test
    @DisplayName("플랜별 공유 가능 여부 검증 - 프로 플랜")
    void canShareForProPlan() {
        CustomApi customApi = CustomApi.builder()
                .apiId("api-12345")
                .userId("user-12345")
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(15)
                .build();

        assertThat(customApi.canShareForPlan("PRO")).isTrue();
    }

    @Test
    @DisplayName("equals와 hashCode 테스트")
    void equalsAndHashCode() {
        CustomApi api1 = CustomApi.builder()
                .apiId("api-12345")
                .userId("user-12345")
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(3)
                .build();

        CustomApi api2 = CustomApi.builder()
                .apiId("api-12345")
                .userId("user-67890")
                .apiName("Different API")
                .description("다른 API")
                .dataCount(5)
                .build();

        assertThat(api1).isEqualTo(api2);
        assertThat(api1.hashCode()).isEqualTo(api2.hashCode());
    }
}