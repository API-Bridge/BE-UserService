package org.example.Usersvc.security;

import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.util.HeaderUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API Gateway에서 전달받은 헤더 정보를 기반으로 인증을 처리하는 필터
 * JWT 검증은 API Gateway에서 이미 완료되었으므로 헤더 정보만 파싱하여 Security Context 설정
 * 
 * 주요 기능:
 * - X-User-Id 헤더에서 사용자 ID 추출
 * - X-User-Email 헤더에서 사용자 이메일 추출
 * - X-User-Roles 헤더에서 사용자 권한 추출
 * - Spring Security Authentication 객체 생성
 * - 헤더가 없는 경우 적절한 에러 응답
 */
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {
    
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLES_HEADER = "X-User-Roles";
    private static final String API_GATEWAY_SOURCE_HEADER = "X-Gateway-Source";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // 공개 엔드포인트는 헤더 검증 생략
        if (isPublicEndpoint(requestPath)) {
            log.debug("공개 엔드포인트 접근: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }
        
        // API Gateway에서 온 요청인지 확인
        String gatewaySource = request.getHeader(API_GATEWAY_SOURCE_HEADER);
        if (gatewaySource == null || !gatewaySource.equals("API-GATEWAY")) {
            log.warn("API Gateway를 통하지 않은 직접 접근 시도: {}", requestPath);
            sendUnauthorizedResponse(response, "Direct access not allowed. Please use API Gateway.");
            return;
        }
        
        // 사용자 ID 헤더 추출
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        String userId = HeaderUtils.extractUserId(userIdHeader);
        
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("X-User-Id 헤더가 누락되었습니다. 경로: {}", requestPath);
            sendUnauthorizedResponse(response, "User ID header is missing");
            return;
        }
        
        // 사용자 이메일 헤더 추출
        String userEmail = request.getHeader(USER_EMAIL_HEADER);
        if (userEmail == null) {
            userEmail = userId + "@api-bridge.com"; // 기본값 설정
        }
        
        // 사용자 권한 헤더 추출
        String userRolesHeader = request.getHeader(USER_ROLES_HEADER);
        List<GrantedAuthority> authorities = parseAuthorities(userRolesHeader, userId);
        
        // Authentication 객체 생성
        HeaderAuthenticationToken authentication = new HeaderAuthenticationToken(
            userId, userEmail, authorities
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        
        // Security Context에 설정
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        log.debug("헤더 기반 인증 성공 - 사용자: {}, 이메일: {}, 권한: {}", 
                 userId, userEmail, authorities.stream()
                     .map(GrantedAuthority::getAuthority)
                     .collect(Collectors.joining(", ")));
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 공개 엔드포인트 여부 확인
     */
    private boolean isPublicEndpoint(String path) {
        String[] publicPaths = {
            "/actuator/", "/v3/api-docs/", "/swagger-ui/", "/swagger-ui.html",
            "/api/health", "/api/subscription/products", "/api/shared-apis",
            "/api/auth/", "/oauth2/", "/login/",
            "/*.html", "/css/", "/js/", "/images/", "/static/", "/public/",
            "/api/webhooks/", "/api/webhook/", "/api/tosspay/webhook/"
        };
        
        return Arrays.stream(publicPaths)
                .anyMatch(publicPath -> path.startsWith(publicPath.replace("*", "")));
    }
    
    /**
     * 사용자 권한 파싱
     */
    private List<GrantedAuthority> parseAuthorities(String rolesHeader, String userId) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 기본 권한 추가
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // 헤더에서 권한 파싱
        if (rolesHeader != null && !rolesHeader.trim().isEmpty()) {
            String[] roles = rolesHeader.split(",");
            for (String role : roles) {
                String trimmedRole = role.trim();
                if (!trimmedRole.isEmpty()) {
                    // ROLE_ 접두사가 없으면 추가
                    if (!trimmedRole.startsWith("ROLE_")) {
                        trimmedRole = "ROLE_" + trimmedRole.toUpperCase();
                    }
                    authorities.add(new SimpleGrantedAuthority(trimmedRole));
                }
            }
        }
        
        // 관리자 사용자인 경우 ADMIN 권한 추가
        if (HeaderUtils.isAdminUser(userId)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        
        return authorities;
    }
    
    /**
     * 401 Unauthorized 응답 전송
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String errorResponse = String.format("""
            {
                "error": "Unauthorized",
                "message": "%s",
                "status": 401,
                "timestamp": "%s"
            }
            """, 
            message,
            java.time.Instant.now().toString()
        );
        
        response.getWriter().write(errorResponse);
        response.getWriter().flush();
    }
}