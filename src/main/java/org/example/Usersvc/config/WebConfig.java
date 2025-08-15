package org.example.Usersvc.config;

import lombok.RequiredArgsConstructor;
import org.example.Usersvc.interceptor.ApiUsageTrackingInterceptor;
import org.example.Usersvc.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 설정 - 인터셉터 등록
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final RateLimitInterceptor rateLimitInterceptor;
    private final ApiUsageTrackingInterceptor apiUsageTrackingInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Rate Limit 인터셉터를 먼저 등록 (우선순위 높음)
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/actuator/**",
                        "/h2-console/**"
                )
                .order(1);
        
        // API 사용량 추적 인터셉터
        registry.addInterceptor(apiUsageTrackingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/actuator/**",
                        "/h2-console/**"
                )
                .order(2);
    }
}