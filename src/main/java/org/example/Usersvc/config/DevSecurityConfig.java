package org.example.Usersvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * 개발 환경용 Spring Security 설정 클래스
 * 개발 편의성을 위해 기본 인증만 사용하고 JWT 검증은 비활성화
 * 
 * 주요 기능:
 * - 기본 HTTP Basic 인증 사용
 * - 모든 API 엔드포인트 접근 허용 (개발 편의성)
 * - CSRF 비활성화
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile({"dev", "default"})
public class DevSecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public DevSecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/**").permitAll()  // 개발 환경에서는 모든 API 허용
                .anyRequest().permitAll()
            )
            .headers(headers -> headers.frameOptions().disable()) // H2 콘솔을 위해 필요
            .httpBasic(basic -> {});  // 기본 HTTP Basic 인증 활성화

        return http.build();
    }
}