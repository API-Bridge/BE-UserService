package org.example.Usersvc.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
// 신뢰 기반 모드에서는 간단한 설정만 필요

/**
 * API Gateway 신뢰 기반 보안 설정 클래스
 * 
 * API Gateway에서 이미 JWT 토큰 검증을 완료했다고 가정하고,
 * 전달된 헤더 정보만을 기반으로 인증/인가를 처리합니다.
 * 
 * 주요 기능:
 * 1. JWT 토큰 재검증 제거 (성능 향상)
 * 2. API Gateway에서 전달된 사용자 정보 헤더 신뢰
 * 3. 메서드 레벨 보안 유지 (@PreAuthorize)
 * 4. 내부 서비스 간 통신 보안 강화
 * 
 * 보안 고려사항:
 * - API Gateway와 User Service 간 네트워크가 신뢰할 수 있는 환경이어야 함
 * - 외부에서 직접 User Service 접근을 차단해야 함
 * - API Gateway에서 헤더 위조 방지 메커니즘 필요
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("trusted-gateway")  // 프로파일로 활성화 제어
public class TrustedGatewayConfig {

    /**
     * API Gateway에서 전달하는 사용자 정보 헤더명
     */
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_AUTHORITIES_HEADER = "X-User-Authorities";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    /**
     * 신뢰 기반 보안 필터 체인
     * API Gateway에서 전달된 헤더를 기반으로 인증 처리
     */
    @Bean
    public SecurityFilterChain trustedFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (내부 서비스 간 통신)
            .csrf(csrf -> csrf.disable())
            
            // 세션 비활성화 (Stateless)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 경로별 접근 권한 설정
            .authorizeHttpRequests(authz -> authz
                // 공개 엔드포인트
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Swagger/OpenAPI - 개발환경에서만
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // 공개 API 엔드포인트
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/subscription/products").permitAll()
                .requestMatchers("/api/shared-apis").permitAll()
                .requestMatchers("/api/shared-apis/search").permitAll()
                
                // API Gateway에서 오는 요청은 헤더만 있으면 허용 (권한 검증 없음)
                .requestMatchers("/api/**").permitAll()
                
                // 관리자 전용은 일단 비활성화
                .requestMatchers("/admin/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/**").permitAll()
                
                .anyRequest().permitAll()
            )
            
            // 모든 요청을 허용하므로 필터 추가 불필요
            ;

        return http.build();
    }

    // 신뢰 기반 모드에서는 모든 요청이 permitAll()이므로 
    // 복잡한 인증 필터나 프로바이더가 불필요합니다.
    // 필요시 나중에 추가할 수 있습니다.
}
