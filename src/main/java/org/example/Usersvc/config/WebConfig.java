package org.example.Usersvc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.interceptor.ApiUsageTrackingInterceptor;
import org.example.Usersvc.interceptor.RateLimitInterceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 설정 - 인터셉터 등록
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final RateLimitInterceptor rateLimitInterceptor;
    private final ApiUsageTrackingInterceptor apiUsageTrackingInterceptor;

    
    @Value("${spring.profiles.active:default}")
    private String activeProfile;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // trusted-gateway 모드에서는 모든 인터셉터 비활성화 (API Gateway에서 처리)
        if ("trusted-gateway".equals(activeProfile)) {
            log.info("trusted-gateway 프로필 활성화 - 모든 인터셉터 비활성화");
            return;
        }
        // 구독 상태 확인 인터셉터는 제거됨
        /*
        registry.addInterceptor(subscriptionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/actuator/**",
                        "/api/auth/**",
                        "/api/user/register",
                        "/api/subscription/**",
                        "/api/shared-apis",
                        "/api/shared-apis/search",
                        "/api/webhook/**",
                        "/h2-console/**"
                )
                .order(0);
        */
        
        // Rate Limit 인터셉터를 두 번째로 등록
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/actuator/**",
                        "/api/subscription/**",
                        "/api/shared-apis",
                        "/api/shared-apis/search",
                        "/h2-console/**"
                )
                .order(1);
        
        // API 사용량 추적 인터셉터
        registry.addInterceptor(apiUsageTrackingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/actuator/**",
                        "/api/subscription/**",
                        "/api/shared-apis",
                        "/api/shared-apis/search",
                        "/h2-console/**"
                )
                .order(2);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 정적 리소스 핸들러 설정
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }
}