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
                .customApiId(apiId)
                .userId(userId)
                .name(apiName)
                .description(description)
                .build();

        assertThat(customApi.getCustomApiId()).isEqualTo(apiId);
        assertThat(customApi.getUserId()).isEqualTo(userId);
        assertThat(customApi.getName()).isEqualTo(apiName);
        assertThat(customApi.getDescription()).isEqualTo(description);
    }

    @Test
    @DisplayName("커스텀 API 정보 업데이트 테스트")
    void updateCustomApiInfo() {
        CustomApi customApi = CustomApi.builder()
                .customApiId("api-12345")
                .userId("user-12345")
                .name("Original API")
                .description("원본 설명")
                .build();

        customApi.updateApiInfo("Updated API", "업데이트된 설명");

        assertThat(customApi.getName()).isEqualTo("Updated API");
        assertThat(customApi.getDescription()).isEqualTo("업데이트된 설명");
    }

    @Test
    @DisplayName("커스텀 API 정보 검증 테스트")
    void validateCustomApiInfo() {
        CustomApi customApi = CustomApi.builder()
                .customApiId("api-12345")
                .userId("user-12345")
                .name("Test API")
                .description("테스트용 API")
                .build();
        customApi.setCreatedAt(LocalDateTime.now());

        assertThat(customApi.isValid()).isTrue();
    }

    @Test
    @DisplayName("커스텀 API 생성시간 설정 테스트")
    void setCreatedAtCustomApi() {
        CustomApi customApi = CustomApi.builder()
                .customApiId("api-12345")
                .userId("user-12345")
                .name("Test API")
                .description("테스트용 API")
                .build();
        
        LocalDateTime now = LocalDateTime.now();
        customApi.setCreatedAt(now);

        assertThat(customApi.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("유효한 커스텀 API 검증 테스트")
    void isValidCustomApi() {
        CustomApi validApi = CustomApi.builder()
                .customApiId("api-12345")
                .userId("user-12345")
                .name("Test API")
                .description("테스트용 API")
                .build();
        validApi.setCreatedAt(LocalDateTime.now());

        assertThat(validApi.isValid()).isTrue();
    }

    @Test
    @DisplayName("무효한 커스텀 API 검증 테스트")
    void isInvalidCustomApi() {
        CustomApi invalidApi = CustomApi.builder()
                .customApiId(null)
                .userId("user-12345")
                .name("Test API")
                .description("테스트용 API")
                .build();

        assertThat(invalidApi.isValid()).isFalse();
    }

    @Test
    @DisplayName("커스텀 API 업데이트 시 공백 제거 테스트")
    void updateApiInfoTrimsWhitespace() {
        CustomApi customApi = CustomApi.builder()
                .customApiId("api-12345")
                .userId("user-12345")
                .name("Test API")
                .description("테스트용 API")
                .build();

        customApi.updateApiInfo("  Updated API  ", "  업데이트된 설명  ");

        assertThat(customApi.getName()).isEqualTo("Updated API");
        assertThat(customApi.getDescription()).isEqualTo("업데이트된 설명");
    }

    @Test
    @DisplayName("equals와 hashCode 테스트")
    void equalsAndHashCode() {
        CustomApi api1 = CustomApi.builder()
                .customApiId("api-12345")
                .userId("user-12345")
                .name("Test API")
                .description("테스트용 API")
                .build();

        CustomApi api2 = CustomApi.builder()
                .customApiId("api-12345")
                .userId("user-67890")
                .name("Different API")
                .description("다른 API")
                .build();

        assertThat(api1).isEqualTo(api2);
        assertThat(api1.hashCode()).isEqualTo(api2.hashCode());
    }
}