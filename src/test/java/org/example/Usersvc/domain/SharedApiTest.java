/* 공유 기능 비활성화 - 테스트 주석 처리
package org.example.Usersvc.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

@DisplayName("공유 API 도메인 테스트")
class SharedApiTest {

    @Test
    @DisplayName("공유 API 생성 테스트")
    void createSharedApi() {
        String sharedApiId = "shared-12345";
        String originalApiId = "api-12345";
        String creatorId = "user-12345";
        String apiName = "Shared Test API";
        String description = "공유된 테스트용 API";
        int dataCount = 3;

        SharedApi sharedApi = SharedApi.builder()
                .sharedApiId(sharedApiId)
                .originalApiId(originalApiId)
                .creatorId(creatorId)
                .apiName(apiName)
                .description(description)
                .dataCount(dataCount)
                .build();

        assertThat(sharedApi.getSharedApiId()).isEqualTo(sharedApiId);
        assertThat(sharedApi.getOriginalApiId()).isEqualTo(originalApiId);
        assertThat(sharedApi.getCreatorId()).isEqualTo(creatorId);
        assertThat(sharedApi.getApiName()).isEqualTo(apiName);
        assertThat(sharedApi.getDescription()).isEqualTo(description);
        assertThat(sharedApi.getDataCount()).isEqualTo(dataCount);
        assertThat(sharedApi.isActive()).isTrue();
    }

    @Test
    @DisplayName("공유 API 비활성화 테스트")
    void deactivateSharedApi() {
        SharedApi sharedApi = SharedApi.builder()
                .sharedApiId("shared-12345")
                .originalApiId("api-12345")
                .creatorId("user-12345")
                .apiName("Shared Test API")
                .description("공유된 테스트용 API")
                .dataCount(3)
                .build();

        sharedApi.deactivate();

        assertThat(sharedApi.isActive()).isFalse();
    }

    @Test
    @DisplayName("공유 API 재활성화 테스트")
    void reactivateSharedApi() {
        SharedApi sharedApi = SharedApi.builder()
                .sharedApiId("shared-12345")
                .originalApiId("api-12345")
                .creatorId("user-12345")
                .apiName("Shared Test API")
                .description("공유된 테스트용 API")
                .dataCount(3)
                .build();

        sharedApi.deactivate();
        sharedApi.reactivate();

        assertThat(sharedApi.isActive()).isTrue();
    }

    @Test
    @DisplayName("공유 API 정보 업데이트 테스트")
    void updateSharedApiInfo() {
        SharedApi sharedApi = SharedApi.builder()
                .sharedApiId("shared-12345")
                .originalApiId("api-12345")
                .creatorId("user-12345")
                .apiName("Original API")
                .description("원본 설명")
                .dataCount(3)
                .build();

        sharedApi.updateInfo("Updated API", "업데이트된 설명");

        assertThat(sharedApi.getApiName()).isEqualTo("Updated API");
        assertThat(sharedApi.getDescription()).isEqualTo("업데이트된 설명");
    }

    @Test
    @DisplayName("유효한 공유 API 검증 테스트")
    void isValidSharedApi() {
        SharedApi validSharedApi = SharedApi.builder()
                .sharedApiId("shared-12345")
                .originalApiId("api-12345")
                .creatorId("user-12345")
                .apiName("Shared Test API")
                .description("공유된 테스트용 API")
                .dataCount(3)
                .build();
        validSharedApi.setCreatedAt(LocalDateTime.now());

        assertThat(validSharedApi.isValid()).isTrue();
    }

    @Test
    @DisplayName("무효한 공유 API 검증 테스트")
    void isInvalidSharedApi() {
        SharedApi invalidSharedApi = SharedApi.builder()
                .sharedApiId(null)
                .originalApiId("api-12345")
                .creatorId("user-12345")
                .apiName("Shared Test API")
                .description("공유된 테스트용 API")
                .dataCount(3)
                .build();

        assertThat(invalidSharedApi.isValid()).isFalse();
    }

    @Test
    @DisplayName("공유 API 생성자 확인 테스트")
    void isCreatedBy() {
        SharedApi sharedApi = SharedApi.builder()
                .sharedApiId("shared-12345")
                .originalApiId("api-12345")
                .creatorId("user-12345")
                .apiName("Shared Test API")
                .description("공유된 테스트용 API")
                .dataCount(3)
                .build();

        assertThat(sharedApi.isCreatedBy("user-12345")).isTrue();
        assertThat(sharedApi.isCreatedBy("user-67890")).isFalse();
    }

    @Test
    @DisplayName("equals와 hashCode 테스트")
    void equalsAndHashCode() {
        SharedApi api1 = SharedApi.builder()
                .sharedApiId("shared-12345")
                .originalApiId("api-12345")
                .creatorId("user-12345")
                .apiName("Shared Test API")
                .description("공유된 테스트용 API")
                .dataCount(3)
                .build();

        SharedApi api2 = SharedApi.builder()
                .sharedApiId("shared-12345")
                .originalApiId("api-67890")
                .creatorId("user-67890")
                .apiName("Different API")
                .description("다른 API")
                .dataCount(5)
                .build();

        assertThat(api1).isEqualTo(api2);
        assertThat(api1.hashCode()).isEqualTo(api2.hashCode());
    }
}*/
