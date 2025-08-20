package org.example.Usersvc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSecretsArn;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.domain.Plan;
import org.example.Usersvc.domain.PlanType;
import org.example.Usersvc.service.UserService;
import org.example.Usersvc.service.UserSecretsArnService;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.example.Usersvc.repository.PlanRepository;
import org.example.Usersvc.common.logging.UserActionLogger;
import org.example.Usersvc.common.logging.SecurityAuditLogger;
import org.example.Usersvc.common.metrics.CustomMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// UserController 테스트 클래스
// TDD 방식으로 UserController의 REST API 엔드포인트들을 테스트합니다.
// 이 테스트는 UserController 구현에 앞서 작성되어 API의 요구사항을 명확히 정의합니다.
//
// 테스트 대상 엔드포인트:
// - POST /api/users - 사용자 생성
// - GET /api/users/{userId} - 사용자 조회  
// - POST /api/users/{userId}/secrets - 개인 키 등록
// - GET /api/secrets/{userId}/arn - 개인 키 조회
// - GET /api/users/{userId}/secrets - 사용자의 모든 키 목록 조회
//
// Mock 객체 사용:
// - UserService: 사용자 관리 비즈니스 로직 시뮬레이션
// - UserSecretsArnService: 암호화 키 관리 비즈니스 로직 시뮬레이션
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserSecretsArnService userSecretsArnService;
    
    @MockBean
    private UserSubscriptionRepository userSubscriptionRepository;
    
    @MockBean
    private PlanRepository planRepository;
    
    @MockBean
    private UserActionLogger userActionLogger;
    
    @MockBean
    private SecurityAuditLogger securityAuditLogger;
    
    @MockBean
    private CustomMetrics customMetrics;

    private User testUser;
    private UserSecretsArn testUserSecretsArn;
    private String testUserId;
    private Plan testPlan;
    private UserSubscription testSubscription;

    // 각 테스트 메서드 실행 전 테스트 데이터 초기화
    // 테스트에 필요한 공통 데이터를 설정하여 일관된 테스트 환경을 제공합니다.
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();
        
        testUser = User.builder()
                .userId(testUserId)
                .auth0Id("auth0|test123456789")
                .userEmail("test@example.com")
                .createdAt(LocalDateTime.now())
                .build();

        testUserSecretsArn = UserSecretsArn.builder()
                .arnId(UUID.randomUUID().toString())
                .userId(testUserId)
                .arn("arn:aws:secretsmanager:us-east-1:123456789012:secret:user-key-AbCdEf")
                .arnDescription("Test encryption key")
                .createdAt(LocalDateTime.now())
                .build();

        testPlan = Plan.builder()
                .planType(PlanType.PRO)
                .price(java.math.BigDecimal.valueOf(22.00))
                .description("Pro plan for testing")
                .features("{\"maxApiCount\":10000}")
                .build();
                
        testSubscription = UserSubscription.builder()
                .subscriptionId(UUID.randomUUID().toString())
                .user(testUser)
                .plan(testPlan)
                .planPaymentDate(LocalDateTime.now())
                .build();
        testSubscription.setIsActive(true);
    }

    // 사용자 생성 API 성공 테스트
    // Auth0에서 받은 사용자 정보로 새 사용자를 생성하는 API를 테스트합니다.
    // 
    // 요청: POST /api/users
    // Body: { "auth0Id": "...", "userEmail": "..." }
    // 응답: 201 Created, 생성된 사용자 정보
    @Test
    @DisplayName("POST /api/users - 사용자 생성 성공")
    @WithMockUser
    void createUser_Success() throws Exception {
        // given: 사용자 생성이 성공하도록 Mock 설정
        when(userService.createUser(anyString(), anyString())).thenReturn(testUser);

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "auth0Id", "auth0|test123456789",
                "userEmail", "test@example.com"
        ));

        // when & then: 사용자 생성 API 호출 및 검증
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(testUser.getUserId()))
                .andExpect(jsonPath("$.data.auth0Id").value(testUser.getAuth0Id()))
                .andExpect(jsonPath("$.data.userEmail").value(testUser.getUserEmail()));

        // Service 호출 검증
        verify(userService).createUser("auth0|test123456789", "test@example.com");
    }

    // 사용자 생성 API 실패 테스트 - 잘못된 요청
    // 필수 필드가 누락된 경우 적절한 에러 응답을 반환하는지 테스트합니다.
    @Test
    @DisplayName("POST /api/users - 잘못된 요청으로 사용자 생성 실패")
    @WithMockUser
    void createUser_BadRequest() throws Exception {
        // given: auth0Id가 누락된 요청
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "userEmail", "test@example.com"
        ));

        // when & then: 400 Bad Request 응답 검증
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Service가 호출되지 않았는지 검증
        verify(userService, never()).createUser(anyString(), anyString());
    }

    // 사용자 조회 API 성공 테스트
    // 사용자 ID로 특정 사용자 정보를 조회하는 API를 테스트합니다.
    //
    // 요청: GET /api/users/{userId}
    // 응답: 200 OK, 사용자 정보
    @Test
    @DisplayName("GET /api/users/{userId} - 사용자 조회 성공")
    @WithMockUser
    void getUser_Success() throws Exception {
        // given: 사용자가 존재하도록 Mock 설정
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));

        // when & then: 사용자 조회 API 호출 및 검증
        mockMvc.perform(get("/api/users/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(testUser.getUserId()))
                .andExpect(jsonPath("$.data.userEmail").value(testUser.getUserEmail()));

        // Service 호출 검증
        verify(userService).getUserById(testUserId);
    }

    // 사용자 조회 API 실패 테스트 - 사용자 없음
    // 존재하지 않는 사용자 ID로 조회 시 404 응답을 반환하는지 테스트합니다.
    @Test
    @DisplayName("GET /api/users/{userId} - 사용자 없음으로 조회 실패")
    @WithMockUser
    void getUser_NotFound() throws Exception {
        // given: 사용자가 존재하지 않도록 Mock 설정
        when(userService.getUserById(testUserId)).thenReturn(Optional.empty());

        // when & then: 404 Not Found 응답 검증
        mockMvc.perform(get("/api/users/{userId}", testUserId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        // Service 호출 검증
        verify(userService).getUserById(testUserId);
    }

    // 개인 키 등록 API 성공 테스트
    // 사용자가 개인 API 키를 등록하는 엔드포인트를 테스트합니다.
    //
    // 요청: POST /api/users/{userId}/secrets
    // Body: { "secretName": "...", "secretValue": "...", "description": "..." }
    // 응답: 201 Created, 등록된 키 정보 (ARN 포함)
    @Test
    @DisplayName("POST /api/users/{userId}/secrets - 개인 키 등록 성공")
    @WithMockUser
    void registerUserSecret_Success() throws Exception {
        // given: 사용자 존재 및 키 등록이 성공하도록 Mock 설정
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(userSecretsArnService.storeUserSecret(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(testUserSecretsArn);

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "secretName", "my-api-key",
                "secretValue", "sk-1234567890abcdef",
                "description", "My personal API key"
        ));

        // when & then: 키 등록 API 호출 및 검증
        mockMvc.perform(post("/api/users/{userId}/secrets", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.arnId").value(testUserSecretsArn.getArnId()))
                .andExpect(jsonPath("$.data.arn").value(testUserSecretsArn.getArn()))
                .andExpect(jsonPath("$.data.arnDescription").value(testUserSecretsArn.getArnDescription()));

        // Service 호출 검증
        verify(userSecretsArnService).storeUserSecret(testUserId, "my-api-key", "sk-1234567890abcdef", "My personal API key");
    }

    // 개인 키 등록 API 실패 테스트 - 잘못된 요청
    // 필수 필드가 누락된 경우 적절한 에러 응답을 반환하는지 테스트합니다.
    @Test
    @DisplayName("POST /api/users/{userId}/secrets - 잘못된 요청으로 키 등록 실패")
    @WithMockUser
    void registerUserSecret_BadRequest() throws Exception {
        // given: secretValue가 누락된 요청
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "secretName", "my-api-key",
                "description", "My personal API key"
        ));

        // when & then: 400 Bad Request 응답 검증
        mockMvc.perform(post("/api/users/{userId}/secrets", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Service가 호출되지 않았는지 검증
        verify(userSecretsArnService, never()).storeUserSecret(anyString(), anyString(), anyString(), anyString());
    }

    // 개인 키 조회 API 성공 테스트
    // AI Service가 개인 키를 요청할 때 ARN을 통해 키를 복호화하여 반환하는 엔드포인트를 테스트합니다.
    //
    // 요청: GET /api/secrets/{userId}/arn?arnId={arnId}
    // 응답: 200 OK, 복호화된 키 값
    @Test
    @DisplayName("GET /api/secrets/{userId}/arn - 개인 키 조회 성공")
    @WithMockUser
    void getUserSecretValue_Success() throws Exception {
        // given: ARN으로 키 조회가 성공하도록 Mock 설정
        String decryptedValue = "sk-1234567890abcdef";
        when(userSecretsArnService.getSecretsArnById(testUserSecretsArn.getArnId()))
                .thenReturn(Optional.of(testUserSecretsArn));
        when(userSecretsArnService.getSecretValue(testUserSecretsArn.getArn()))
                .thenReturn(decryptedValue);

        // when & then: 키 조회 API 호출 및 검증
        mockMvc.perform(get("/api/secrets/{userId}/arn", testUserId)
                        .param("arnId", testUserSecretsArn.getArnId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.secretValue").value(decryptedValue))
                .andExpect(jsonPath("$.data.arnId").value(testUserSecretsArn.getArnId()));

        // Service 호출 검증
        verify(userSecretsArnService).getSecretsArnById(testUserSecretsArn.getArnId());
        verify(userSecretsArnService).getSecretValue(testUserSecretsArn.getArn());
    }

    // 개인 키 조회 API 실패 테스트 - ARN 없음
    // 존재하지 않는 ARN ID로 조회 시 404 응답을 반환하는지 테스트합니다.
    @Test
    @DisplayName("GET /api/secrets/{userId}/arn - ARN 없음으로 키 조회 실패")
    @WithMockUser
    void getUserSecretValue_NotFound() throws Exception {
        // given: ARN이 존재하지 않도록 Mock 설정
        when(userSecretsArnService.getSecretsArnById(testUserSecretsArn.getArnId()))
                .thenReturn(Optional.empty());

        // when & then: 404 Not Found 응답 검증
        mockMvc.perform(get("/api/secrets/{userId}/arn", testUserId)
                        .param("arnId", testUserSecretsArn.getArnId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        // Service 호출 검증
        verify(userSecretsArnService).getSecretsArnById(testUserSecretsArn.getArnId());
        verify(userSecretsArnService, never()).getSecretValue(anyString());
    }

    // 사용자의 모든 키 목록 조회 API 테스트
    // 특정 사용자가 등록한 모든 암호화 키의 목록을 조회하는 엔드포인트를 테스트합니다.
    //
    // 요청: GET /api/users/{userId}/secrets
    // 응답: 200 OK, 사용자의 키 목록 (ARN 정보 포함, 실제 키 값은 제외)
    @Test
    @DisplayName("GET /api/users/{userId}/secrets - 사용자 키 목록 조회 성공")
    @WithMockUser
    void getUserSecrets_Success() throws Exception {
        // given: 사용자 존재 및 여러 키를 소유하도록 Mock 설정
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        UserSecretsArn secondArn = UserSecretsArn.builder()
                .arnId(UUID.randomUUID().toString())
                .userId(testUserId)
                .arn("arn:aws:secretsmanager:us-east-1:123456789012:secret:user-key-2-XyZwVu")
                .arnDescription("Backup API key")
                .createdAt(LocalDateTime.now())
                .build();

        List<UserSecretsArn> userSecrets = Arrays.asList(testUserSecretsArn, secondArn);
        when(userSecretsArnService.getUserSecretsArns(testUserId)).thenReturn(userSecrets);

        // when & then: 키 목록 조회 API 호출 및 검증
        mockMvc.perform(get("/api/users/{userId}/secrets", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].arnId").value(testUserSecretsArn.getArnId()))
                .andExpect(jsonPath("$.data[0].arnDescription").value(testUserSecretsArn.getArnDescription()))
                .andExpect(jsonPath("$.data[1].arnId").value(secondArn.getArnId()))
                .andExpect(jsonPath("$.data[1].arnDescription").value(secondArn.getArnDescription()));

        // Service 호출 검증
        verify(userSecretsArnService).getUserSecretsArns(testUserId);
    }

    // 권한 없는 접근 테스트
    // 인증되지 않은 사용자가 API에 접근할 때 401 응답을 반환하는지 테스트합니다.
    @Test
    @DisplayName("인증되지 않은 사용자의 API 접근 시 401 Unauthorized")
    void unauthorizedAccess_ShouldReturn401() throws Exception {
        // when & then: 인증 없이 API 호출 시 401 응답 검증
        mockMvc.perform(get("/api/users/{userId}", testUserId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/users/{userId}/secrets", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // 잘못된 사용자 ID 형식 테스트
    // UUID가 아닌 잘못된 형식의 사용자 ID로 요청 시 400 응답을 반환하는지 테스트합니다.
    @Test
    @DisplayName("잘못된 사용자 ID 형식으로 요청 시 400 Bad Request")
    @WithMockUser
    void invalidUserIdFormat_ShouldReturn400() throws Exception {
        // given: 잘못된 형식의 사용자 ID
        String invalidUserId = "invalid-uuid-format";

        // when & then: 400 Bad Request 응답 검증
        mockMvc.perform(get("/api/users/{userId}", invalidUserId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // 플랜 기능 조회 API 성공 테스트 - Pro 플랜
    @Test
    @DisplayName("GET /api/users/plan-features - Pro 플랜 기능 조회 성공")
    @WithMockUser
    void getPlanFeatures_ProPlan_Success() throws Exception {
        // given: Pro 플랜을 사용하는 사용자
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(userSubscriptionRepository.findActiveSubscriptionByUser(testUser))
                .thenReturn(Optional.of(testSubscription));

        // when & then: 플랜 기능 조회 API 호출 및 검증
        mockMvc.perform(get("/api/users/plan-features")
                        .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planType").value("PRO"))
                .andExpect(jsonPath("$.data.planName").value("Pro"))
                .andExpect(jsonPath("$.data.price").exists())
                .andExpect(jsonPath("$.data.features.maxCustomApiCount").exists())
                .andExpect(jsonPath("$.data.features.maxSharedApiCount").exists())
                .andExpect(jsonPath("$.data.features.rateLimitPerMinute").exists());

        verify(userService).getUserById(testUserId);
        verify(userSubscriptionRepository).findActiveSubscriptionByUser(testUser);
    }

    // 플랜 기능 조회 API 성공 테스트 - Free 플랜 (구독 없음)
    @Test
    @DisplayName("GET /api/users/plan-features - Free 플랜 기능 조회 성공")
    @WithMockUser
    void getPlanFeatures_FreePlan_Success() throws Exception {
        // given: 활성 구독이 없는 사용자 (Free 플랜)
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(userSubscriptionRepository.findActiveSubscriptionByUser(testUser))
                .thenReturn(Optional.empty());

        // when & then: 플랜 기능 조회 API 호출 및 검증
        mockMvc.perform(get("/api/users/plan-features")
                        .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planType").value("FREE"))
                .andExpect(jsonPath("$.data.planName").value("Free"))
                .andExpect(jsonPath("$.data.price").value(0.0))
                .andExpect(jsonPath("$.data.description").value("기본 무료 플랜"))
                .andExpect(jsonPath("$.data.features.maxCustomApiCount").exists())
                .andExpect(jsonPath("$.data.features.maxSharedApiCount").exists())
                .andExpect(jsonPath("$.data.features.rateLimitPerMinute").exists());

        verify(userService).getUserById(testUserId);
        verify(userSubscriptionRepository).findActiveSubscriptionByUser(testUser);
    }

    // 플랜 기능 조회 API 실패 테스트 - 사용자 없음
    @Test
    @DisplayName("GET /api/users/plan-features - 사용자 없음으로 조회 실패")
    @WithMockUser
    void getPlanFeatures_UserNotFound() throws Exception {
        // given: 사용자가 존재하지 않음
        when(userService.getUserById(testUserId)).thenReturn(Optional.empty());

        // when & then: 404 Not Found 응답 검증
        mockMvc.perform(get("/api/users/plan-features")
                        .header("X-User-Id", testUserId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));

        verify(userService).getUserById(testUserId);
        verify(userSubscriptionRepository, never()).findActiveSubscriptionByUser(any());
    }

    // 플랜 기능 조회 API 실패 테스트 - 사용자 ID 없음
    @Test
    @DisplayName("GET /api/users/plan-features - X-User-Id 헤더 없음으로 조회 실패")
    @WithMockUser
    void getPlanFeatures_NoUserId() throws Exception {
        // when & then: 400 Bad Request 응답 검증
        mockMvc.perform(get("/api/users/plan-features"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_USER_ID"));

        verify(userService, never()).getUserById(anyString());
        verify(userSubscriptionRepository, never()).findActiveSubscriptionByUser(any());
    }
}