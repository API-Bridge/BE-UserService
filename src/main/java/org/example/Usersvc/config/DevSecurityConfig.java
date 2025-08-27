package org.example.Usersvc.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * 개발 환경용 Spring Security 설정 클래스
 * JWT 인증을 지원하되 개발 편의성을 위해 완화된 보안 설정 사용
 * 
 * 주요 기능:
 * - JWT 토큰 인증 지원 (API Gateway 연동)
 * - 개발 편의성을 위한 완화된 권한 검사
 * - H2 콘솔, Swagger 등 개발 도구 접근 허용
 * - CSRF 비활성화
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile({"dev", "default", "trusted-gateway"})
public class DevSecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    @Value("${auth0.audience:https://api.api-bridge.com}")
    private String audience;

    @Value("${auth0.issuerUri:https://api-bridge.us.auth0.com/}")
    private String issuer;

    public DevSecurityConfig(@Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)) // OAuth2 Login을 위해 필요시 세션 생성
            .authorizeHttpRequests(authz -> authz
                // 공개 엔드포인트
                .requestMatchers("/actuator/**", "/h2-console/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                
                // 인증 관련 경로는 공개 (Auth0 OAuth2 로그인)
                .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**").permitAll()
                
                // 정적 리소스 허용 (테스트 페이지)
                .requestMatchers("/", "/index.html", "/*.html").permitAll()
                .requestMatchers("/static/**", "/public/**").permitAll()
                .requestMatchers("/payment-test.html", "/payment-success.html", "/payment-cancel.html").permitAll()
                .requestMatchers("/shared-api-test.html", "/tosspay-test.html").permitAll()
                .requestMatchers("/health", "/api/health").permitAll()
                
                // API 엔드포인트 허용
                .requestMatchers("/api/subscription/checkout").permitAll()
                .requestMatchers("/api/subscription/cancel").permitAll()
                .requestMatchers("/api/subscription/success").permitAll()
                .requestMatchers("/api/subscription/products").permitAll()
                .requestMatchers("/api/subscription/debug").permitAll()
                .requestMatchers("/api/users/*/subscription").permitAll()
                .requestMatchers("/api/webhooks/**").permitAll()
                .requestMatchers("/api/webhook/**").permitAll()
                .requestMatchers("/api/tosspay/webhook/**").permitAll()
                .requestMatchers("/api/tosspay/**").permitAll()
                .requestMatchers("/webhook").permitAll()
                
                // 통합 결제 엔드포인트 허용
                .requestMatchers("/api/payments/**").permitAll()
                .requestMatchers("/payment-test").permitAll()
                .requestMatchers("/payment-test/**").permitAll()
                
                // 공유 API 엔드포인트 허용
                .requestMatchers("/api/shared-apis/**").permitAll()
                
                // 개발 편의성: 테스트 엔드포인트 허용
                .requestMatchers("/users/test-headers", "/users/me").permitAll()
                .requestMatchers("/test/**").permitAll()  // 구독 테스트 페이지 허용
                .requestMatchers("/simple/**").permitAll()  // 간단한 테스트 페이지
                
                // 기타 모든 요청 - JWT 인증 또는 완화된 접근
                .anyRequest().permitAll()  // 개발 환경에서는 JWT가 없어도 허용
            )
                          .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable())) // H2 콘솔 지원
            // OAuth2 Login 설정 (Auth0 로그인 처리)
            .oauth2Login(oauth2 -> oauth2
                .successHandler((request, response, authentication) -> {
                    // 로그인 성공 시 login-success 페이지로 리다이렉트
                    response.sendRedirect("/api/auth/login-success");
                })
                .failureHandler((request, response, exception) -> {
                    // 로그인 실패 시 error 페이지로 리다이렉트
                    response.sendRedirect("/api/auth/login-error");
                })
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                // JWT 인증 실패 시 적절한 JSON 에러 응답 반환
                // 
                // *** AuthenticationEntryPoint 호출 원리 ***
                // 이 람다식은 Filter Chain에 등록되지만 조건부로만 실행됨:
                //
                // 1. SUCCESS PATH (이 람다식 호출 안됨):
                //    BearerTokenAuthenticationFilter → JWT 추출 성공
                //    → JwtAuthenticationProvider → jwtDecoder().decode() 성공
                //    → DetailedAudienceValidator.validate() → success() 반환
                //    → JwtAuthenticationConverter → 권한 변환
                //    → SecurityContext에 Authentication 저장
                //    → 다음 필터로 진행 (UserController 도달)
                //
                // 2. FAILURE PATH (이 람다식 호출됨):
                //    BearerTokenAuthenticationFilter → JWT 추출 또는 검증 실패
                //    → JwtAuthenticationProvider에서 AuthenticationException 발생
                //    → ExceptionTranslationFilter가 Exception catch
                //    → authenticationEntryPoint.commence() 호출 ← 바로 여기서 이 람다식 실행
                //    → Filter Chain 중단, 즉시 응답 반환
                //
                // 즉, Spring Security는 인증 성공 시에는 이 EntryPoint를 호출하지 않고,
                // 인증 실패 Exception이 발생했을 때만 ExceptionTranslationFilter를 통해 호출함
                .authenticationEntryPoint((request, response, authException) -> {
                    // JWT 인증 실패 상세 로깅
                    String requestPath = request.getRequestURI();
                    String authHeader = request.getHeader("Authorization");
                    
                    log.warn("=== JWT 인증 실패 디버깅 정보 ===");
                    log.warn("요청 경로: {}", requestPath);
                    log.warn("Authorization 헤더 존재: {}", authHeader != null);
                    if (authHeader != null) {
                        log.warn("Authorization 헤더 형식: {}", authHeader.length() > 20 ? authHeader.substring(0, 20) + "..." : authHeader);
                    }
                    log.warn("인증 예외 유형: {}", authException.getClass().getSimpleName());
                    log.warn("인증 예외 메시지: {}", authException.getMessage());
                    log.warn("설정된 Auth0 Issuer: {}", issuer);
                    log.warn("설정된 Auth0 Audience: {}", audience);
                    log.warn("================================");
                    
                    // JSON 에러 응답 반환
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    
                    String errorResponse = """
                        {
                            "error": "Unauthorized",
                            "message": "Authentication required. Please provide a valid JWT token.",
                            "status": 401,
                            "timestamp": "%s",
                            "path": "%s",
                            "details": {
                                "reason": "%s",
                                "authHeaderPresent": %b
                            }
                        }
                        """.formatted(
                            java.time.Instant.now().toString(),
                            requestPath,
                            authException.getMessage(),
                            authHeader != null
                        );
                    
                    try {
                        response.getWriter().write(errorResponse);
                        response.getWriter().flush();
                    } catch (IOException e) {
                        log.error("JWT 인증 실패 응답 작성 중 오류 발생", e);
                    }
                })
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            log.info("=== JWT 디코더 초기화 ===");
            log.info("Auth0 Issuer URI: {}", issuer);
            log.info("JWK Set URI: {}", issuer + ".well-known/jwks.json");
            log.info("Expected Audience: {}", audience);
            
            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                    .withJwkSetUri(issuer + ".well-known/jwks.json")
                    .build();

            OAuth2TokenValidator<Jwt> audienceValidator = new DetailedAudienceValidator(audience);
            OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
            OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

            jwtDecoder.setJwtValidator(withAudience);
            
            log.info("JWT 디코더 초기화 성공");
            log.info("========================");
            
            return jwtDecoder;
        } catch (Exception e) {
            log.error("=== JWT 디코더 초기화 실패 ===");
            log.error("오류 유형: {}", e.getClass().getSimpleName());
            log.error("오류 메시지: {}", e.getMessage());
            log.error("JWK Set URI 연결 실패 가능성");
            log.error("============================");
            
            // 개발 환경에서 Auth0 연결 실패 시 기본 디코더 반환
            return NimbusJwtDecoder.withJwkSetUri(issuer + ".well-known/jwks.json").build();
        }
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter permissionsConverter = new JwtGrantedAuthoritiesConverter();
        permissionsConverter.setAuthoritiesClaimName("permissions");
        permissionsConverter.setAuthorityPrefix("");

        JwtGrantedAuthoritiesConverter rolesConverter = new JwtGrantedAuthoritiesConverter();
        rolesConverter.setAuthoritiesClaimName("roles");
        rolesConverter.setAuthorityPrefix("ROLE_");

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
        jwtConverter.setPrincipalClaimName("sub");

        return jwtConverter;
    }

    /**
     * Auth0 Audience 검증을 위한 커스텀 Validator (개발용) - 상세 로깅 포함
     */
    public static class DetailedAudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final String expectedAudience;
        // Auth0 Client ID도 유효한 audience로 허용 (하위 호환성)
        private static final String AUTH0_CLIENT_ID = "gleGDUmC5iSKvbt4IN12t7cAIHfwEYQI";

        public DetailedAudienceValidator(String expectedAudience) {
            this.expectedAudience = expectedAudience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            log.debug("=== JWT Audience 검증 시작 ===");
            log.debug("JWT Subject: {}", jwt.getSubject());
            log.debug("JWT Issuer: {}", jwt.getIssuer() != null ? jwt.getIssuer().toString() : "null");
            log.debug("JWT Issued At: {}", jwt.getIssuedAt());
            log.debug("JWT Expires At: {}", jwt.getExpiresAt());
            log.debug("JWT Audience 목록: {}", jwt.getAudience());
            log.debug("기대하는 Audience: {} 또는 Client ID: {}", expectedAudience, AUTH0_CLIENT_ID);
            
            if (jwt.getAudience() != null) {
                // API Identifier 또는 Client ID를 audience로 허용
                if (jwt.getAudience().contains(expectedAudience) || jwt.getAudience().contains(AUTH0_CLIENT_ID)) {
                    String matchedAudience = jwt.getAudience().contains(expectedAudience) ? expectedAudience : AUTH0_CLIENT_ID;
                    log.debug("Audience 검증 성공: {} 포함됨", matchedAudience);
                    log.debug("============================");
                    return OAuth2TokenValidatorResult.success();
                }
            }
            
            // 개발 환경에서는 audience 검증 실패를 경고로만 처리
            log.warn("=== JWT Audience 검증 실패 ===");
            log.warn("기대 Audience: {} 또는 Client ID: {}", expectedAudience, AUTH0_CLIENT_ID);
            log.warn("실제 JWT Audience: {}", jwt.getAudience());
            log.warn("JWT의 다른 클레임들:");
            jwt.getClaims().forEach((key, value) -> {
                if (!key.equals("aud")) { // audience는 이미 로깅했으므로 제외
                    log.warn("  {}: {}", key, value);
                }
            });
            log.warn("개발 모드: 계속 진행");
            log.warn("===========================");
            
            OAuth2Error error = new OAuth2Error("invalid_audience", 
                "The required audience is missing: " + expectedAudience + " or " + AUTH0_CLIENT_ID, null);
            return OAuth2TokenValidatorResult.failure(error); // 인증 실패로 처리
        }
    }
}