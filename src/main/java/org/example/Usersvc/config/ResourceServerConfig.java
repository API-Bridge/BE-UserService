package org.example.Usersvc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * User Service OAuth2 Resource Server 설정 클래스
 * 
 * API Gateway에서 전달받은 Auth0 JWT 토큰을 검증하고 인가 처리를 수행합니다.
 * Spring MVC 기반의 마이크로서비스에서 JWT 인증을 처리합니다.
 * 
 * 주요 기능:
 * 1. JWT 토큰 검증 (서명, 만료시간, audience 등)
 * 2. Auth0 클레임을 Spring Security 권한으로 변환
 * 3. 메서드 레벨 보안 지원 (@PreAuthorize, @PostAuthorize)
 * 4. 경로별 접근 제어
 * 5. Stateless 인증 (세션 비사용)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@org.springframework.context.annotation.Profile({"staging", "legacy-jwt"})
public class ResourceServerConfig {

    @Value("${auth0.audience}")
    private String audience;

    @Value("${auth0.issuerUri}")
    private String issuer;

    /**
     * Spring Security 필터 체인 설정
     * JWT Resource Server로 동작하도록 구성
     * 
     * @param http HttpSecurity 빌더
     * @return SecurityFilterChain 보안 필터 체인
     * @throws Exception 설정 오류
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 토큰 사용)
            .csrf(csrf -> csrf.disable())
            
            // 세션 비활성화 (Stateless JWT 인증)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 경로별 접근 권한 설정
            .authorizeHttpRequests(authz -> authz
                // 공개 엔드포인트 - 인증 불필요
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // CORS preflight
                
                // Swagger/OpenAPI 문서 - 개발 환경에서만 허용
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // 사용자 관련 API - 권한별 접근 제어
                .requestMatchers(HttpMethod.GET, "/users/profile").hasAnyAuthority("read:users", "read:profile")
                .requestMatchers(HttpMethod.PUT, "/users/profile").hasAnyAuthority("write:users", "write:profile")
                .requestMatchers(HttpMethod.GET, "/users/**").hasAuthority("read:users")
                .requestMatchers(HttpMethod.POST, "/users/**").hasAuthority("write:users")
                .requestMatchers(HttpMethod.PUT, "/users/**").hasAuthority("write:users")
                .requestMatchers(HttpMethod.DELETE, "/users/**").hasAuthority("delete:users")
                
                // 관리자 전용 엔드포인트
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/actuator/**").hasRole("ADMIN")
                
                // 기타 모든 요청 - 인증 필요
                .anyRequest().authenticated()
            )
            
            // OAuth2 Resource Server 설정
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                // 인증 실패 시 JSON 응답
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"unauthorized\",\"message\":\"Valid JWT token required\",\"status\":401}"
                    );
                })
                // 권한 부족 시 JSON 응답
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"insufficient_scope\",\"message\":\"Insufficient permissions\",\"status\":403}"
                    );
                })
            );

        return http.build();
    }

    /**
     * JWT 디코더 설정
     * Auth0에서 발급한 JWT 토큰을 검증하고 디코딩합니다.
     * 
     * @return JwtDecoder JWT 디코더
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // Auth0 JWKS 엔드포인트에서 JWT 디코더 생성
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withJwkSetUri(issuer + ".well-known/jwks.json")
                .build();

        // JWT 검증 설정 (Issuer + Audience)
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);

        return jwtDecoder;
    }

    /**
     * JWT 인증 변환기 설정
     * JWT 클레임을 Spring Security Authentication 객체로 변환합니다.
     * 
     * @return JwtAuthenticationConverter JWT 인증 변환기
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Auth0 permissions 클레임 변환기
        JwtGrantedAuthoritiesConverter permissionsConverter = new JwtGrantedAuthoritiesConverter();
        permissionsConverter.setAuthoritiesClaimName("permissions");
        permissionsConverter.setAuthorityPrefix("");

        // Auth0 roles 클레임 변환기
        JwtGrantedAuthoritiesConverter rolesConverter = new JwtGrantedAuthoritiesConverter();
        rolesConverter.setAuthoritiesClaimName("roles");
        rolesConverter.setAuthorityPrefix("ROLE_");

        // 두 변환기를 결합
        Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter = jwt -> {
            var permissions = permissionsConverter.convert(jwt);
            var roles = rolesConverter.convert(jwt);
            
            return Stream.concat(
                permissions != null ? permissions.stream() : Stream.empty(),
                roles != null ? roles.stream() : Stream.empty()
            ).toList();
        };

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        
        // JWT의 'sub' 클레임을 principal name으로 사용
        jwtConverter.setPrincipalClaimName("sub");

        return jwtConverter;
    }

    /**
     * Auth0 Audience 검증을 위한 커스텀 Validator
     */
    public static class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final String expectedAudience;

        public AudienceValidator(String expectedAudience) {
            this.expectedAudience = expectedAudience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            if (jwt.getAudience() != null && jwt.getAudience().contains(expectedAudience)) {
                return OAuth2TokenValidatorResult.success();
            }
            
            OAuth2Error error = new OAuth2Error("invalid_audience", 
                "The required audience is missing: " + expectedAudience, null);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}
