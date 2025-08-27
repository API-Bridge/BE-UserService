package org.example.Usersvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.example.Usersvc.service.UserService;
import org.example.Usersvc.domain.User;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Auth0 OAuth2 로그인 처리를 위한 컨트롤러
 * User Service에서 직접 Auth0 로그인을 처리합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Auth0 OAuth2 인증 관련 API")
public class AuthController {

    @Value("${auth0.issuerUri}")
    private String issuerUri;

    @Value("${auth0.client-id:}")
    private String clientId;

    @Value("${auth0.audience}")
    private String audience;

    @Value("${auth0.logout-redirect-uri:http://localhost:8081/api/auth/logout-success}")
    private String logoutRedirectUri;

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Auth0 로그인 시작
     */
    @GetMapping("/login")
    @Operation(summary = "Auth0 로그인 시작", description = "Auth0 OAuth2 로그인 시작")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "OAuth2 로그인 시작으로 리다이렉트")
    })
    public ResponseEntity<Void> login() {
        log.info("Auth0 로그인 시작 요청");
        
        return ResponseEntity.status(302)
                .location(URI.create("/oauth2/authorization/auth0"))
                .build();
    }

    /**
     * OAuth2 로그인 성공 후 사용자 정보 반환
     */
    @GetMapping("/login-success")
    @Operation(summary = "로그인 성공 후 사용자 정보", description = "로그인 성공 후 사용자 정보 반환 (id token, access token 포함)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "사용자 정보 반환 성공")
    })
    public ResponseEntity<Map<String, Object>> loginSuccess(
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser principal) {
        
        if (principal == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "로그인이 필요합니다");
            response.put("loginUrl", "/api/auth/login");
            response.put("status", "not_authenticated");
            return ResponseEntity.ok(response);
        }

        try {
            // OAuth2 사용자 정보로부터 데이터 추출
            String auth0Id = principal.getSubject();  // Auth0 고유 ID (sub claim)
            String email = principal.getEmail();

            // 기존 createUser 메서드 활용 - 이미 중복 체크 로직이 포함되어 있음
            // createUser 메서드는 이미 존재하는 사용자면 기존 사용자를 반환하고,
            // 없으면 새로 생성하여 반환함
            User user = userService.createUser(auth0Id, email);
            
            log.info("OAuth2 로그인 사용자 처리 완료 - userId: {}, auth0Id: {}", user.getUserId(), auth0Id);

            // 응답 데이터 구성
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("message", "Auth0 로그인 성공!");
            userInfo.put("userId", user.getUserId());  // DB에서 생성된 실제 사용자 ID
            userInfo.put("auth0Id", auth0Id);
            userInfo.put("email", user.getUserEmail());
            userInfo.put("name", principal.getFullName());
            userInfo.put("picture", principal.getPicture());
            userInfo.put("emailVerified", principal.getEmailVerified());
            userInfo.put("authorities", principal.getAuthorities());
            userInfo.put("status", "authenticated");

            // ID Token을 API 호출용으로 사용 (Management API JWE 문제 회피)
            String apiToken = null;
            if (principal.getIdToken() != null) {
                apiToken = principal.getIdToken().getTokenValue();
                userInfo.put("accessToken", apiToken);  // ID Token을 Access Token으로 사용
                userInfo.put("jwtToken", apiToken);     // API 호출용 JWT 토큰
                log.info("사용자 {} 로그인 성공 - ID Token을 API 토큰으로 사용 (Management API 회피)", user.getUserEmail());
                log.info("API 요청 시 Authorization: Bearer {} 형식으로 사용하세요", apiToken.substring(0, 20) + "...");
            }
            
            // OAuth2AuthorizedClientService 사용 중단 (Management API 호출 방지)
            // 이 서비스가 내부적으로 Management API (JWE)를 호출하여 문제 발생

            // OIDC id_token - 사용자 식별용
            if (principal.getIdToken() != null) {
                String idToken = principal.getIdToken().getTokenValue();
                userInfo.put("idToken", idToken);
                log.debug("ID Token 발급됨 (사용자 식별용) - userId: {}", user.getUserId());
            }
            
            // 사용 안내 추가
            userInfo.put("usage", "API 요청 시 'jwtToken' (Access Token)을 Authorization 헤더에 Bearer 토큰으로 사용하세요");

            return ResponseEntity.ok(userInfo);
            
        } catch (IllegalArgumentException e) {
            // 이메일 중복 등 비즈니스 로직 오류
            log.error("OAuth2 로그인 처리 중 오류: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "user_creation_failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            // 기타 예상치 못한 오류
            log.error("OAuth2 로그인 처리 중 시스템 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "internal_error");
            errorResponse.put("message", "로그인 처리 중 오류가 발생했습니다");
            errorResponse.put("status", "error");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * OAuth2 로그인 실패 시 에러 정보 반환
     */
    @GetMapping("/login-error")
    @Operation(summary = "로그인 실패 정보", description = "로그인 실패 시 에러 정보 반환")
    @ApiResponses({
        @ApiResponse(responseCode = "400", description = "로그인 실패")
    })
    public ResponseEntity<Map<String, Object>> loginError() {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "로그인 실패");
        error.put("message", "Auth0 로그인 중 오류가 발생했습니다");
        error.put("retryUrl", "/api/auth/login");

        return ResponseEntity.status(400).body(error);
    }

    /**
     * 현재 인증된 사용자 정보 조회
     */
    @GetMapping("/user-info")
    @Operation(summary = "현재 사용자 정보 조회", description = "현재 로그인된 사용자 정보 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "사용자 정보 반환 성공")
    })
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser principal) {
        
        if (principal == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", false);
            response.put("message", "인증되지 않은 사용자");
            response.put("loginUrl", "/api/auth/login");
            return ResponseEntity.ok(response);
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("authenticated", true);
        userInfo.put("userId", principal.getSubject());
        userInfo.put("email", principal.getEmail());
        userInfo.put("name", principal.getFullName());
        userInfo.put("picture", principal.getPicture());
        userInfo.put("emailVerified", principal.getEmailVerified());
        userInfo.put("authorities", principal.getAuthorities());
        
        return ResponseEntity.ok(userInfo);
    }

    /**
     * 로그아웃 엔드포인트
     */
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "로컬 세션 종료")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    public ResponseEntity<Map<String, Object>> logout(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "로그아웃이 성공적으로 완료되었습니다");
        response.put("status", "logged_out");
        response.put("loginUrl", "/api/auth/login");
        response.put("timestamp", java.time.Instant.now().toString());

        if (jwt != null) {
            response.put("userId", jwt.getClaimAsString("sub"));
            log.info("사용자 {} 로그아웃 완료", jwt.getClaimAsString("sub"));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃 성공 후 처리
     */
    @GetMapping("/logout-success")
    @Operation(summary = "로그아웃 성공", description = "로그아웃 성공 후 처리")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그아웃 성공 메시지")
    })
    public ResponseEntity<Map<String, Object>> logoutSuccess() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "로그아웃이 성공적으로 완료되었습니다");
        response.put("status", "logged_out");
        response.put("loginUrl", "/api/auth/login");
        response.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 클라이언트에서 사용할 Auth0 설정 정보 반환
     */
    @GetMapping("/config")
    @Operation(summary = "Auth0 설정 정보 조회", description = "클라이언트용 Auth0 퍼블릭 설정 정보 반환")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Auth0 설정 정보를 성공적으로 반환했습니다")
    })
    public ResponseEntity<Map<String, String>> getAuth0Config() {
        // issuerUri에서 domain 추출 (https://domain.auth0.com/ -> domain.auth0.com)
        String domain = issuerUri.replaceAll("^https?://", "").replaceAll("/$", "");

        Map<String, String> config = Map.of(
                "domain", domain,
                "clientId", clientId != null ? clientId : "",
                "audience", audience
        );

        return ResponseEntity.ok(config);
    }
}