package org.example.Usersvc.service;

import org.example.Usersvc.domain.CustomApi;
import org.example.Usersvc.domain.SharedApi;
import org.example.Usersvc.repository.CustomApiRepository;
import org.example.Usersvc.repository.SharedApiRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("공유 API 서비스 테스트")
class SharedApiServiceTest {

    @Mock
    private SharedApiRepository sharedApiRepository;

    @Mock
    private CustomApiRepository customApiRepository;

    @Mock
    private ApiSharingValidationService apiSharingValidationService;

    @InjectMocks
    private SharedApiService sharedApiService;

    @Test
    @DisplayName("API 공유 게시 성공")
    void shareApi_Success() {
        String userId = "user-12345";
        String apiId = "api-12345";

        CustomApi customApi = CustomApi.builder()
                .apiId(apiId)
                .userId(userId)
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(3)
                .build();

        SharedApi expectedSharedApi = SharedApi.builder()
                .sharedApiId(any(String.class))
                .originalApiId(apiId)
                .creatorId(userId)
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(3)
                .build();

        when(customApiRepository.findByApiIdAndDeletedFalse(apiId)).thenReturn(Optional.of(customApi));
        when(apiSharingValidationService.validateApiForSharing(customApi, "FREE")).thenReturn(true);
        when(sharedApiRepository.findByOriginalApiIdAndActiveTrue(apiId)).thenReturn(Optional.empty());
        when(sharedApiRepository.save(any(SharedApi.class))).thenReturn(expectedSharedApi);

        SharedApi result = sharedApiService.shareApi(userId, apiId, "FREE");

        assertThat(result).isNotNull();
        assertThat(result.getOriginalApiId()).isEqualTo(apiId);
        assertThat(result.getCreatorId()).isEqualTo(userId);
        assertThat(result.getApiName()).isEqualTo("Test API");

        verify(customApiRepository).findByApiIdAndDeletedFalse(apiId);
        verify(apiSharingValidationService).validateApiForSharing(customApi, "FREE");
        verify(sharedApiRepository).findByOriginalApiIdAndActiveTrue(apiId);
        verify(sharedApiRepository).save(any(SharedApi.class));
    }

    @Test
    @DisplayName("존재하지 않는 API 공유 시도 시 예외 발생")
    void shareApi_ApiNotFound() {
        String userId = "user-12345";
        String apiId = "non-existent-api";

        when(customApiRepository.findByApiIdAndDeletedFalse(apiId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharedApiService.shareApi(userId, apiId, "FREE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("API를 찾을 수 없습니다.");

        verify(customApiRepository).findByApiIdAndDeletedFalse(apiId);
        verifyNoInteractions(apiSharingValidationService);
        verifyNoInteractions(sharedApiRepository);
    }

    @Test
    @DisplayName("다른 사용자의 API 공유 시도 시 예외 발생")
    void shareApi_UnauthorizedUser() {
        String userId = "user-12345";
        String otherUserId = "user-67890";
        String apiId = "api-12345";

        CustomApi otherUsersApi = CustomApi.builder()
                .apiId(apiId)
                .userId(otherUserId)
                .apiName("Other User API")
                .description("다른 사용자의 API")
                .dataCount(2)
                .build();

        when(customApiRepository.findByApiIdAndDeletedFalse(apiId)).thenReturn(Optional.of(otherUsersApi));

        assertThatThrownBy(() -> sharedApiService.shareApi(userId, apiId, "FREE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인의 API만 공유할 수 있습니다.");

        verify(customApiRepository).findByApiIdAndDeletedFalse(apiId);
        verifyNoInteractions(apiSharingValidationService);
    }

    @Test
    @DisplayName("공유 불가능한 API 공유 시도 시 예외 발생")
    void shareApi_ValidationFailed() {
        String userId = "user-12345";
        String apiId = "api-12345";

        CustomApi customApi = CustomApi.builder()
                .apiId(apiId)
                .userId(userId)
                .apiName("Large API")
                .description("큰 데이터 API")
                .dataCount(10)
                .build();

        when(customApiRepository.findByApiIdAndDeletedFalse(apiId)).thenReturn(Optional.of(customApi));
        when(apiSharingValidationService.validateApiForSharing(customApi, "FREE")).thenReturn(false);

        assertThatThrownBy(() -> sharedApiService.shareApi(userId, apiId, "FREE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현재 플랜에서는 이 API를 공유할 수 없습니다.");

        verify(customApiRepository).findByApiIdAndDeletedFalse(apiId);
        verify(apiSharingValidationService).validateApiForSharing(customApi, "FREE");
    }

    @Test
    @DisplayName("이미 공유된 API 재공유 시도 시 예외 발생")
    void shareApi_AlreadyShared() {
        String userId = "user-12345";
        String apiId = "api-12345";

        CustomApi customApi = CustomApi.builder()
                .apiId(apiId)
                .userId(userId)
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(3)
                .build();

        SharedApi existingSharedApi = SharedApi.builder()
                .sharedApiId("shared-12345")
                .originalApiId(apiId)
                .creatorId(userId)
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(3)
                .build();

        when(customApiRepository.findByApiIdAndDeletedFalse(apiId)).thenReturn(Optional.of(customApi));
        when(apiSharingValidationService.validateApiForSharing(customApi, "FREE")).thenReturn(true);
        when(sharedApiRepository.findByOriginalApiIdAndActiveTrue(apiId)).thenReturn(Optional.of(existingSharedApi));

        assertThatThrownBy(() -> sharedApiService.shareApi(userId, apiId, "FREE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 공유된 API입니다.");

        verify(customApiRepository).findByApiIdAndDeletedFalse(apiId);
        verify(apiSharingValidationService).validateApiForSharing(customApi, "FREE");
        verify(sharedApiRepository).findByOriginalApiIdAndActiveTrue(apiId);
    }

    @Test
    @DisplayName("공유 API 비활성화 성공")
    void unshareApi_Success() {
        String userId = "user-12345";
        String apiId = "api-12345";

        SharedApi sharedApi = SharedApi.builder()
                .sharedApiId("shared-12345")
                .originalApiId(apiId)
                .creatorId(userId)
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(3)
                .build();

        when(sharedApiRepository.findByOriginalApiIdAndActiveTrue(apiId)).thenReturn(Optional.of(sharedApi));
        when(sharedApiRepository.save(any(SharedApi.class))).thenReturn(sharedApi);

        sharedApiService.unshareApi(userId, apiId);

        verify(sharedApiRepository).findByOriginalApiIdAndActiveTrue(apiId);
        verify(sharedApiRepository).save(sharedApi);
        assertThat(sharedApi.isActive()).isFalse();
    }

    @Test
    @DisplayName("공유되지 않은 API 비활성화 시도 시 예외 발생")
    void unshareApi_NotShared() {
        String userId = "user-12345";
        String apiId = "api-12345";

        when(sharedApiRepository.findByOriginalApiIdAndActiveTrue(apiId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharedApiService.unshareApi(userId, apiId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("공유되지 않은 API입니다.");

        verify(sharedApiRepository).findByOriginalApiIdAndActiveTrue(apiId);
        verify(sharedApiRepository, never()).save(any());
    }

    @Test
    @DisplayName("다른 사용자의 공유 API 비활성화 시도 시 예외 발생")
    void unshareApi_UnauthorizedUser() {
        String userId = "user-12345";
        String otherUserId = "user-67890";
        String apiId = "api-12345";

        SharedApi sharedApi = SharedApi.builder()
                .sharedApiId("shared-12345")
                .originalApiId(apiId)
                .creatorId(otherUserId)
                .apiName("Test API")
                .description("테스트용 API")
                .dataCount(3)
                .build();

        when(sharedApiRepository.findByOriginalApiIdAndActiveTrue(apiId)).thenReturn(Optional.of(sharedApi));

        assertThatThrownBy(() -> sharedApiService.unshareApi(userId, apiId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인이 공유한 API만 취소할 수 있습니다.");

        verify(sharedApiRepository).findByOriginalApiIdAndActiveTrue(apiId);
        verify(sharedApiRepository, never()).save(any());
    }
}