package org.example.Usersvc.config;

import lombok.RequiredArgsConstructor;
import org.example.Usersvc.interceptor.ApiUsageTrackingInterceptor;
import org.example.Usersvc.interceptor.RateLimitInterceptor;
import org.example.Usersvc.interceptor.SubscriptionInterceptor;
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
    private final SubscriptionInterceptor subscriptionInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 구독 상태 확인 인터셉터를 가장 먼저 등록 (최우선)
        registry.addInterceptor(subscriptionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/actuator/**",
                        "/api/auth/**",
                        "/api/user/register",
                        "/api/subscription/**",
                        "/api/webhook/**",
                        "/h2-console/**"
                )
                .order(0);
        
        // Rate Limit 인터셉터를 두 번째로 등록
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