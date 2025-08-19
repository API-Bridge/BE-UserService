package org.example.Usersvc.config;

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

    public DevSecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // 공개 엔드포인트
                .requestMatchers("/actuator/**", "/h2-console/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                
                // 정적 리소스 허용 (테스트 페이지)
                .requestMatchers("/", "/index.html", "/*.html").permitAll()
                .requestMatchers("/static/**", "/public/**").permitAll()
                .requestMatchers("/payment-test.html", "/payment-success.html", "/payment-cancel.html").permitAll()
                .requestMatchers("/shared-api-test.html").permitAll()
                .requestMatchers("/health", "/api/health").permitAll()
                
                // API 엔드포인트 허용
                .requestMatchers("/api/subscription/checkout").permitAll()
                .requestMatchers("/api/subscription/cancel").permitAll()
                .requestMatchers("/api/subscription/products").permitAll()
                .requestMatchers("/api/subscription/debug").permitAll()
                .requestMatchers("/api/users/*/subscription").permitAll()
                .requestMatchers("/api/webhooks/**").permitAll()
                .requestMatchers("/webhook").permitAll()
                
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
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                // 개발 환경에서는 JWT 인증 실패 시에도 계속 진행
                .authenticationEntryPoint((request, response, authException) -> {
                    // JWT가 없거나 유효하지 않아도 허용
                    response.setStatus(200);
                })
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                    .withJwkSetUri(issuer + ".well-known/jwks.json")
                    .build();

            OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
            OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
            OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

            jwtDecoder.setJwtValidator(withAudience);
            return jwtDecoder;
        } catch (Exception e) {
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
     * Auth0 Audience 검증을 위한 커스텀 Validator (개발용)
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
            
            // 개발 환경에서는 audience 검증 실패를 경고로만 처리
            OAuth2Error error = new OAuth2Error("invalid_audience", 
                "The required audience is missing: " + expectedAudience + " (dev mode)", null);
            return OAuth2TokenValidatorResult.success(); // 개발 환경에서는 계속 진행
        }
    }
}