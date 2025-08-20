package org.example.Usersvc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Usersvc.domain.PlanType;
import org.example.Usersvc.domain.SharedApi;
import org.example.Usersvc.domain.UserSavedApi;
import org.example.Usersvc.service.SharedApiService;
import org.example.Usersvc.service.UserSavedApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SharedApiController.class)
class SharedApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SharedApiService sharedApiService;

    @MockBean
    private UserSavedApiService userSavedApiService;

    private SharedApi testSharedApi;
    private UserSavedApi testUserSavedApi;
    private String testUserId;
    private String testCustomApiId;
    private String testSharedApiId;

    @BeforeEach
    void setUp() {
        testUserId = "user-001";
        testCustomApiId = "custom-api-001";
        testSharedApiId = UUID.randomUUID().toString();

        testSharedApi = SharedApi.builder()
                .sharedApiId(testSharedApiId)
                .originalApiId(testCustomApiId)
                .creatorId(testUserId)
                .apiName("Test Shared API")
                .description("Test description")
                .dataCount(100)
                .build();
        testSharedApi.setCreatedAt(LocalDateTime.now());

        testUserSavedApi = UserSavedApi.builder()
                .userApiId(UUID.randomUUID().toString())
                .userId(testUserId)
                .sharedApiId(testSharedApiId)
                .apiName("Saved API")
                .description("Saved description")
                .dataCount(50)
                .build();
    }

    @Test
    @DisplayName("POST /api/users/{userId}/shared-apis/share - API 공유 성공")
    @WithMockUser
    void shareApi_Success() throws Exception {
        // given
        when(sharedApiService.shareApi(eq(testUserId), eq(testCustomApiId), eq(PlanType.PRO), 
                eq("Test API"), eq("Test description")))
                .thenReturn(testSharedApi);

        // when & then
        mockMvc.perform(post("/api/users/{userId}/shared-apis/share", testUserId)
                        .param("customApiId", testCustomApiId)
                        .param("planType", "PRO")
                        .param("apiName", "Test API")
                        .param("apiDescription", "Test description")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sharedApiId").value(testSharedApi.getSharedApiId()))
                .andExpect(jsonPath("$.data.originalApiId").value(testSharedApi.getOriginalApiId()))
                .andExpect(jsonPath("$.data.apiName").value(testSharedApi.getApiName()));

        verify(sharedApiService).shareApi(testUserId, testCustomApiId, PlanType.PRO, "Test API", "Test description");
    }

    @Test
    @DisplayName("POST /api/users/{userId}/shared-apis/share - API 공유 실패 (이미 공유된 API)")
    @WithMockUser
    void shareApi_AlreadyShared() throws Exception {
        // given
        when(sharedApiService.shareApi(eq(testUserId), eq(testCustomApiId), eq(PlanType.FREE), 
                anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("이미 공유된 API입니다."));

        // when & then
        mockMvc.perform(post("/api/users/{userId}/shared-apis/share", testUserId)
                        .param("customApiId", testCustomApiId)
                        .param("planType", "FREE")
                        .param("apiName", "Test API")
                        .param("apiDescription", "Test description")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 공유된 API입니다."))
                .andExpect(jsonPath("$.errorCode").value("SHARE_API_ERROR"));

        verify(sharedApiService).shareApi(testUserId, testCustomApiId, PlanType.FREE, "Test API", "Test description");
    }

    @Test
    @DisplayName("DELETE /api/users/{userId}/shared-apis/unshare/{sharedApiId} - API 공유 취소 성공")
    @WithMockUser
    void unshareApiBySharedId_Success() throws Exception {
        // given
        doNothing().when(sharedApiService).unshareApiBySharedId(testUserId, testSharedApiId);

        // when & then
        mockMvc.perform(delete("/api/users/{userId}/shared-apis/unshare/{sharedApiId}", 
                        testUserId, testSharedApiId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(sharedApiService).unshareApiBySharedId(testUserId, testSharedApiId);
    }

    @Test
    @DisplayName("DELETE /api/users/{userId}/shared-apis/unshare/{sharedApiId} - API 공유 취소 실패")
    @WithMockUser
    void unshareApiBySharedId_Failed() throws Exception {
        // given
        doThrow(new IllegalArgumentException("공유되지 않은 API입니다."))
                .when(sharedApiService).unshareApiBySharedId(testUserId, testSharedApiId);

        // when & then
        mockMvc.perform(delete("/api/users/{userId}/shared-apis/unshare/{sharedApiId}", 
                        testUserId, testSharedApiId)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("공유되지 않은 API입니다."))
                .andExpect(jsonPath("$.errorCode").value("UNSHARE_API_ERROR"));

        verify(sharedApiService).unshareApiBySharedId(testUserId, testSharedApiId);
    }

    @Test
    @DisplayName("GET /api/users/{userId}/shared-apis/my - 내가 공유한 API 목록 조회")
    @WithMockUser
    void getMySharedApis_Success() throws Exception {
        // given
        List<SharedApi> mySharedApis = Arrays.asList(testSharedApi);
        when(sharedApiService.getSharedApisByCreator(testUserId)).thenReturn(mySharedApis);

        // when & then
        mockMvc.perform(get("/api/users/{userId}/shared-apis/my", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].sharedApiId").value(testSharedApi.getSharedApiId()))
                .andExpect(jsonPath("$.data[0].apiName").value(testSharedApi.getApiName()));

        verify(sharedApiService).getSharedApisByCreator(testUserId);
    }

    @Test
    @DisplayName("POST /api/users/{userId}/shared-apis/save - 공유 API 저장 성공")
    @WithMockUser
    void saveSharedApi_Success() throws Exception {
        // given
        when(userSavedApiService.saveSharedApi(testUserId, testSharedApiId))
                .thenReturn(testUserSavedApi);

        // when & then
        mockMvc.perform(post("/api/users/{userId}/shared-apis/save", testUserId)
                        .param("sharedApiId", testSharedApiId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userApiId").value(testUserSavedApi.getUserApiId()))
                .andExpect(jsonPath("$.data.sharedApiId").value(testUserSavedApi.getSharedApiId()));

        verify(userSavedApiService).saveSharedApi(testUserId, testSharedApiId);
    }

    @Test
    @DisplayName("POST /api/users/{userId}/shared-apis/save - 공유 API 저장 실패 (이미 저장됨)")
    @WithMockUser
    void saveSharedApi_AlreadySaved() throws Exception {
        // given
        when(userSavedApiService.saveSharedApi(testUserId, testSharedApiId))
                .thenThrow(new IllegalArgumentException("이미 저장된 API입니다."));

        // when & then
        mockMvc.perform(post("/api/users/{userId}/shared-apis/save", testUserId)
                        .param("sharedApiId", testSharedApiId)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 저장된 API입니다."))
                .andExpect(jsonPath("$.errorCode").value("SAVE_API_ERROR"));

        verify(userSavedApiService).saveSharedApi(testUserId, testSharedApiId);
    }

    @Test
    @DisplayName("GET /api/users/{userId}/shared-apis/saved - 저장된 API 목록 조회")
    @WithMockUser
    void getSavedApis_Success() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<UserSavedApi> savedApisPage = new PageImpl<>(Arrays.asList(testUserSavedApi), pageable, 1);
        when(userSavedApiService.getUserSavedApis(eq(testUserId), any(Pageable.class)))
                .thenReturn(savedApisPage);

        // when & then
        mockMvc.perform(get("/api/users/{userId}/shared-apis/saved", testUserId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].userApiId").value(testUserSavedApi.getUserApiId()));

        verify(userSavedApiService).getUserSavedApis(eq(testUserId), any(Pageable.class));
    }

    @Test
    @DisplayName("DELETE /api/users/{userId}/shared-apis/saved/{userApiId} - 저장된 API 삭제 성공")
    @WithMockUser
    void deleteSavedApi_Success() throws Exception {
        // given
        String userApiId = testUserSavedApi.getUserApiId();
        doNothing().when(userSavedApiService).deleteSavedApi(testUserId, userApiId);

        // when & then
        mockMvc.perform(delete("/api/users/{userId}/shared-apis/saved/{userApiId}", 
                        testUserId, userApiId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userSavedApiService).deleteSavedApi(testUserId, userApiId);
    }

    @Test
    @DisplayName("DELETE /api/users/{userId}/shared-apis/saved/{userApiId} - 저장된 API 삭제 실패")
    @WithMockUser
    void deleteSavedApi_Failed() throws Exception {
        // given
        String userApiId = testUserSavedApi.getUserApiId();
        doThrow(new IllegalArgumentException("저장된 API를 찾을 수 없습니다."))
                .when(userSavedApiService).deleteSavedApi(testUserId, userApiId);

        // when & then
        mockMvc.perform(delete("/api/users/{userId}/shared-apis/saved/{userApiId}", 
                        testUserId, userApiId)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("저장된 API를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.errorCode").value("DELETE_SAVED_API_ERROR"));

        verify(userSavedApiService).deleteSavedApi(testUserId, userApiId);
    }
}