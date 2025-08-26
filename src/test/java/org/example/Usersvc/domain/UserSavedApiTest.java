/* 공유 기능 비활성화 - 테스트 주석 처리
package org.example.Usersvc.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

@DisplayName("사용자 저장 API 도메인 테스트")
class UserSavedApiTest {

    @Test
    @DisplayName("사용자 저장 API 생성 테스트")
    void createUserSavedApi() {
        String userApiId = "user-api-12345";
        String userId = "user-12345";
        String sharedApiId = "shared-12345";
        String apiName = "Saved Test API";
        String description = "저장된 테스트용 API";
        int dataCount = 3;

        UserSavedApi userSavedApi = UserSavedApi.builder()
                .userApiId(userApiId)
                .userId(userId)
                .sharedApiId(sharedApiId)
                .apiName(apiName)
                .description(description)
                .dataCount(dataCount)
                .build();

        assertThat(userSavedApi.getUserApiId()).isEqualTo(userApiId);
        assertThat(userSavedApi.getUserId()).isEqualTo(userId);
        assertThat(userSavedApi.getSharedApiId()).isEqualTo(sharedApiId);
        assertThat(userSavedApi.getApiName()).isEqualTo(apiName);
        assertThat(userSavedApi.getDescription()).isEqualTo(description);
        assertThat(userSavedApi.getDataCount()).isEqualTo(dataCount);
        assertThat(userSavedApi.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("사용자 저장 API 정보 업데이트 테스트")
    void updateUserSavedApiInfo() {
        UserSavedApi userSavedApi = UserSavedApi.builder()
                .userApiId("user-api-12345")
                .userId("user-12345")
                .sharedApiId("shared-12345")
                .apiName("Original API")
                .description("원본 설명")
                .dataCount(3)
                .build();

        userSavedApi.updateApiInfo("Updated API", "업데이트된 설명");

        assertThat(userSavedApi.getApiName()).isEqualTo("Updated API");
        assertThat(userSavedApi.getDescription()).isEqualTo("업데이트된 설명");
    }

    @Test
    @DisplayName("사용자 저장 API 삭제(Soft Delete) 테스트")
    void softDeleteUserSavedApi() {
        UserSavedApi userSavedApi = UserSavedApi.builder()
                .userApiId("user-api-12345")
                .userId("user-12345")
                .sharedApiId("shared-12345")
                .apiName("Saved Test API")
                .description("저장된 테스트용 API")
                .dataCount(3)
                .build();

        userSavedApi.markAsDeleted();

        assertThat(userSavedApi.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("유효한 사용자 저장 API 검증 테스트")
    void isValidUserSavedApi() {
        UserSavedApi validApi = UserSavedApi.builder()
                .userApiId("user-api-12345")
                .userId("user-12345")
                .sharedApiId("shared-12345")
                .apiName("Saved Test API")
                .description("저장된 테스트용 API")
                .dataCount(3)
                .build();
        validApi.setCreatedAt(LocalDateTime.now());

        assertThat(validApi.isValid()).isTrue();
    }

    @Test
    @DisplayName("무효한 사용자 저장 API 검증 테스트")
    void isInvalidUserSavedApi() {
        UserSavedApi invalidApi = UserSavedApi.builder()
                .userApiId(null)
                .userId("user-12345")
                .sharedApiId("shared-12345")
                .apiName("Saved Test API")
                .description("저장된 테스트용 API")
                .dataCount(3)
                .build();

        assertThat(invalidApi.isValid()).isFalse();
    }

    @Test
    @DisplayName("사용자 소유 확인 테스트")
    void isOwnedBy() {
        UserSavedApi userSavedApi = UserSavedApi.builder()
                .userApiId("user-api-12345")
                .userId("user-12345")
                .sharedApiId("shared-12345")
                .apiName("Saved Test API")
                .description("저장된 테스트용 API")
                .dataCount(3)
                .build();

        assertThat(userSavedApi.isOwnedBy("user-12345")).isTrue();
        assertThat(userSavedApi.isOwnedBy("user-67890")).isFalse();
    }

    @Test
    @DisplayName("공유 API 기반 확인 테스트")
    void isBasedOnSharedApi() {
        UserSavedApi userSavedApi = UserSavedApi.builder()
                .userApiId("user-api-12345")
                .userId("user-12345")
                .sharedApiId("shared-12345")
                .apiName("Saved Test API")
                .description("저장된 테스트용 API")
                .dataCount(3)
                .build();

        assertThat(userSavedApi.isBasedOnSharedApi("shared-12345")).isTrue();
        assertThat(userSavedApi.isBasedOnSharedApi("shared-67890")).isFalse();
    }

    @Test
    @DisplayName("equals와 hashCode 테스트")
    void equalsAndHashCode() {
        UserSavedApi api1 = UserSavedApi.builder()
                .userApiId("user-api-12345")
                .userId("user-12345")
                .sharedApiId("shared-12345")
                .apiName("Saved Test API")
                .description("저장된 테스트용 API")
                .dataCount(3)
                .build();

        UserSavedApi api2 = UserSavedApi.builder()
                .userApiId("user-api-12345")
                .userId("user-67890")
                .sharedApiId("shared-67890")
                .apiName("Different API")
                .description("다른 API")
                .dataCount(5)
                .build();

        assertThat(api1).isEqualTo(api2);
        assertThat(api1.hashCode()).isEqualTo(api2.hashCode());
    }
}*/
