package org.example.Usersvc.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * User Service 테스트 컨트롤러
 * 
 * 다운스트림 서비스에서 JWT 토큰 전달 및 검증을 확인하기 위한 엔드포인트
 */
@RestController
@RequestMapping("/users")
public class TestController {

    /**
     * 사용자 정보 조회 엔드포인트
     * 스모크 테스트: GET /api/users/me → User Service의 /users/me
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication,
            HttpServletRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "USER-SERVICE");
        response.put("status", "success");
        response.put("message", "User information retrieved successfully");
        response.put("authenticated", authentication.isAuthenticated());
        response.put("principal", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        
        // 헤더 정보 확인
        Map<String, String> headers = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> 
            headers.put(headerName, request.getHeader(headerName))
        );
        response.put("receivedHeaders", headers);
        
        // JWT 토큰 정보
        if (jwt != null) {
            response.put("jwt", Map.of(
                "subject", jwt.getSubject(),
                "issuer", jwt.getIssuer().toString(),
                "audience", jwt.getAudience(),
                "expiresAt", jwt.getExpiresAt(),
                "issuedAt", jwt.getIssuedAt(),
                "tokenRelaySuccess", true,
                "permissions", jwt.getClaimAsStringList("permissions"),
                "roles", jwt.getClaimAsStringList("roles")
            ));
        } else {
            response.put("jwt", Map.of("tokenRelaySuccess", false));
        }
        
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 권한별 테스트 엔드포인트
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "USER-SERVICE");
        response.put("endpoint", "/users/profile");
        response.put("status", "success");
        response.put("message", "User profile accessed successfully");
        response.put("requiredPermissions", new String[]{"read:users", "read:profile"});
        response.put("userPermissions", authentication.getAuthorities());
        
        if (jwt != null) {
            response.put("subject", jwt.getSubject());
            response.put("email", jwt.getClaimAsString("email"));
            response.put("name", jwt.getClaimAsString("name"));
        }
        
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 헤더 검사 전용 엔드포인트
     * TokenRelay가 제대로 동작하는지 확인
     */
    @GetMapping("/test-headers")
    public ResponseEntity<Map<String, Object>> testHeaders(
            HttpServletRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "USER-SERVICE");
        response.put("endpoint", "/users/test-headers");
        
        // 모든 헤더 정보 수집
        Map<String, String> allHeaders = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> 
            allHeaders.put(headerName.toLowerCase(), request.getHeader(headerName))
        );
        
        response.put("allHeaders", allHeaders);
        response.put("hasAuthorization", allHeaders.containsKey("authorization"));
        response.put("hasXGateway", allHeaders.containsKey("x-gateway"));
        response.put("authorizationHeader", allHeaders.get("authorization"));
        
        // JWT 검증 결과
        if (jwt != null) {
            response.put("jwtDecoded", true);
            response.put("tokenSource", "Successfully decoded from Authorization header");
            response.put("subject", jwt.getSubject());
        } else {
            response.put("jwtDecoded", false);
            response.put("tokenSource", "No JWT token found or failed to decode");
        }
        
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}

