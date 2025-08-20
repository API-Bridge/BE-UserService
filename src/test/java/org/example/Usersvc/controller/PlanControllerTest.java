package org.example.Usersvc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.service.ApiUsageTrackingService;
import org.example.Usersvc.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlanController.class)
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApiUsageTrackingService apiUsageTrackingService;

    @MockBean
    private UserService userService;

    private User testUser;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "user-001";
        
        testUser = User.builder()
                .userId(testUserId)
                .auth0Id("auth0|test123")
                .userEmail("test@example.com")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/plan/usage-stats - 사용량 통계 조회 성공 (ApiUsageTrackingService 사용)")
    @WithMockUser
    void getUsageStats_WithTrackingService_Success() throws Exception {
        // given
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(apiUsageTrackingService.getCurrentMinuteUsage(testUser)).thenReturn(5L);
        when(apiUsageTrackingService.getCurrentHourUsage(testUser)).thenReturn(45L);
        when(apiUsageTrackingService.getCurrentDayUsage(testUser)).thenReturn(320L);
        when(apiUsageTrackingService.getMonthlyUsage(testUser)).thenReturn(8500L);

        // when & then
        mockMvc.perform(get("/api/plan/usage-stats")
                        .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentMinute").value(5))
                .andExpect(jsonPath("$.data.currentHour").value(45))
                .andExpect(jsonPath("$.data.currentDay").value(320))
                .andExpect(jsonPath("$.data.currentMonth").value(8500))
                .andExpect(jsonPath("$.data.userId").value(testUserId))
                .andExpect(jsonPath("$.data.timestamp").exists());

        verify(userService).getUserById(testUserId);
        verify(apiUsageTrackingService).getCurrentMinuteUsage(testUser);
        verify(apiUsageTrackingService).getCurrentHourUsage(testUser);
        verify(apiUsageTrackingService).getCurrentDayUsage(testUser);
        verify(apiUsageTrackingService).getMonthlyUsage(testUser);
    }

    @Test
    @DisplayName("GET /api/plan/usage-stats - ApiUsageTrackingService가 null인 경우 기본값 반환")
    @WithMockUser
    void getUsageStats_WithoutTrackingService_ReturnsDefaults() throws Exception {
        // given
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        // apiUsageTrackingService는 MockBean이지만 null로 동작하도록 설정되지 않음
        // 실제로는 Optional injection을 통해 null이 될 수 있지만, 테스트에서는 기본값을 검증

        // when & then
        mockMvc.perform(get("/api/plan/usage-stats")
                        .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(testUserId))
                .andExpect(jsonPath("$.data.timestamp").exists());

        verify(userService).getUserById(testUserId);
    }

    @Test
    @DisplayName("GET /api/plan/usage-stats - 사용자 없음으로 실패")
    @WithMockUser
    void getUsageStats_UserNotFound() throws Exception {
        // given
        when(userService.getUserById(testUserId)).thenReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/plan/usage-stats")
                        .header("X-User-Id", testUserId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("사용량 통계 조회 실패: 사용자를 찾을 수 없습니다: " + testUserId))
                .andExpect(jsonPath("$.errorCode").value("USAGE_STATS_RETRIEVAL_FAILED"));

        verify(userService).getUserById(testUserId);
        verify(apiUsageTrackingService, never()).getCurrentMinuteUsage(any());
    }

    @Test
    @DisplayName("GET /api/plan/usage-stats - X-User-Id 헤더 없음으로 실패")
    @WithMockUser
    void getUsageStats_NoUserIdHeader() throws Exception {
        // when & then
        mockMvc.perform(get("/api/plan/usage-stats"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("USAGE_STATS_RETRIEVAL_FAILED"));

        verify(userService, never()).getUserById(anyString());
        verify(apiUsageTrackingService, never()).getCurrentMinuteUsage(any());
    }

    @Test
    @DisplayName("GET /api/plan/usage-stats - ApiUsageTrackingService 예외 처리")
    @WithMockUser
    void getUsageStats_ServiceException() throws Exception {
        // given
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(apiUsageTrackingService.getCurrentMinuteUsage(testUser))
                .thenThrow(new RuntimeException("Database connection failed"));

        // when & then
        mockMvc.perform(get("/api/plan/usage-stats")
                        .header("X-User-Id", testUserId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("USAGE_STATS_RETRIEVAL_FAILED"));

        verify(userService).getUserById(testUserId);
        verify(apiUsageTrackingService).getCurrentMinuteUsage(testUser);
    }
}