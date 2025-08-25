package org.example.Usersvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.service.ApiUsageTrackingService;
import org.example.Usersvc.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API 사용량 추적 인터셉터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiUsageTrackingInterceptor implements HandlerInterceptor {
    
    private final UserService userService;
    private final ApiUsageTrackingService apiUsageTrackingService;
    
    private static final String START_TIME_ATTRIBUTE = "startTime";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // 사용량 추적에서 제외할 엔드포인트들
            String requestUri = request.getRequestURI();
            if (shouldSkipTracking(requestUri)) {
                return true;
            }
            
            // 사용자 정보 추출
            Optional<User> user = extractUser(request);
            if (user.isEmpty()) {
                // 사용자 정보가 없으면 추적하지 않음
                return true;
            }
            
            // API 사용량 기록
            String apiEndpoint = normalizeEndpoint(requestUri);
            apiUsageTrackingService.recordApiCall(user.get(), apiEndpoint);
            
            log.debug("API 사용량 추적됨 - userId: {}, endpoint: {}, method: {}", 
                    user.get().getUserId(), apiEndpoint, request.getMethod());
                    
        } catch (Exception e) {
            log.warn("API 사용량 추적 실패: {}", e.getMessage());
            // 사용량 추적 실패가 전체 요청을 방해하지 않도록 true 반환
        }
        
        return true;
    }
    
    /**
     * 사용자 정보 추출
     * API Gateway에서 전달된 X-User-Id 헤더에서 사용자 정보를 추출합니다.
     */
    private Optional<User> extractUser(HttpServletRequest request) {
        try {
            // X-User-Id 헤더에서 추출 (API Gateway에서 처리된 경우)
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null && !userIdHeader.trim().isEmpty()) {
                return userService.getUserById(userIdHeader.trim());
            }
            
        } catch (Exception e) {
            log.debug("사용자 정보 추출 실패: {}", e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * 사용량 추적에서 제외할 엔드포인트인지 확인
     */
    private boolean shouldSkipTracking(String requestUri) {
        // Health check, 정적 리소스, 내부 API 등은 추적하지 않음
        return requestUri.startsWith("/actuator/") ||
               requestUri.startsWith("/api/health") ||
               requestUri.startsWith("/swagger-") ||
               requestUri.startsWith("/v3/api-docs") ||
               requestUri.startsWith("/webjars/") ||
               requestUri.startsWith("/static/") ||
               requestUri.startsWith("/css/") ||
               requestUri.startsWith("/js/") ||
               requestUri.startsWith("/images/") ||
               requestUri.startsWith("/favicon.ico") ||
               requestUri.startsWith("/test/") ||
               requestUri.equals("/");
    }

    /**
     * API 엔드포인트 정규화
     * 경로 파라미터를 일반화하여 같은 API로 인식되도록 합니다.
     */
    private String normalizeEndpoint(String requestUri) {
        // 공통 패턴들을 정규화
        String normalized = requestUri
                // UUID 패턴을 {id}로 변경
                .replaceAll("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "/{id}")
                // user-XXX 패턴을 {userId}로 변경
                .replaceAll("/user-[0-9]+", "/{userId}")
                // 숫자 ID를 {id}로 변경
                .replaceAll("/[0-9]+", "/{id}")
                // API 키나 ARN 패턴 변경
                .replaceAll("/arn-[a-zA-Z0-9]+", "/{arnId}")
                .replaceAll("/api-[a-zA-Z0-9]+", "/{apiId}")
                .replaceAll("/shared-[a-zA-Z0-9]+", "/{sharedApiId}");
        
        // 쿼리 파라미터 제거
        int queryIndex = normalized.indexOf('?');
        if (queryIndex > 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        
        return normalized;
    }
}