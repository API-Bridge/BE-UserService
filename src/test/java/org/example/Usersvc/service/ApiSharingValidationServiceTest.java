package org.example.Usersvc.service;

import org.example.Usersvc.domain.CustomApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("API 공유 검증 서비스 테스트")
class ApiSharingValidationServiceTest {

    @InjectMocks
    private ApiSharingValidationService apiSharingValidationService;

    @Test
    @DisplayName("플랜별 공유 가능한 최대 데이터 수 검증")
    void validateMaxDataCountByPlan() {
        assertThat(apiSharingValidationService.getMaxDataCountForPlan("FREE")).isEqualTo(3);
        assertThat(apiSharingValidationService.getMaxDataCountForPlan("PRO")).isEqualTo(20);
        assertThat(apiSharingValidationService.getMaxDataCountForPlan("ENTERPRISE")).isEqualTo(Integer.MAX_VALUE);
        assertThat(apiSharingValidationService.getMaxDataCountForPlan("UNKNOWN")).isEqualTo(0);
    }

    @Test
    @DisplayName("API 공유 유효성 검증")
    void validateApiForSharing() {
        CustomApi validApi = CustomApi.builder()
                .apiId("api-valid")
                .userId("user-12345")
                .apiName("Valid API")
                .description("유효한 API")
                .dataCount(2)
                .build();

        CustomApi deletedApi = CustomApi.builder()
                .apiId("api-deleted")
                .userId("user-12345")
                .apiName("Deleted API")
                .description("삭제된 API")
                .dataCount(2)
                .build();
        deletedApi.markAsDeleted();

        assertThat(apiSharingValidationService.validateApiForSharing(validApi, "FREE")).isTrue();
        assertThat(apiSharingValidationService.validateApiForSharing(deletedApi, "FREE")).isFalse();
        assertThat(apiSharingValidationService.validateApiForSharing(null, "FREE")).isFalse();

        CustomApi largeApi = CustomApi.builder()
                .apiId("api-large")
                .userId("user-12345")
                .apiName("Large API")
                .description("큰 API")
                .dataCount(10)
                .build();

        assertThat(apiSharingValidationService.validateApiForSharing(largeApi, "FREE")).isFalse();
        assertThat(apiSharingValidationService.validateApiForSharing(largeApi, "PRO")).isTrue();
    }
}