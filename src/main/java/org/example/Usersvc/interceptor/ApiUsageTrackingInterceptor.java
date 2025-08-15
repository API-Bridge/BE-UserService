package org.example.Usersvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.service.ApiUsageService;
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
    private final ApiUsageService apiUsageService;
    
    private static final String START_TIME_ATTRIBUTE = "startTime";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 요청 시작 시간 기록
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        try {
            // 사용자 정보 추출
            String userIdHeader = request.getHeader("X-User-ID");
            if (userIdHeader == null) {
                return; // 인증되지 않은 요청은 기록하지 않음
            }
            
            Long userId = Long.parseLong(userIdHeader);
            User user = userService.findByUserId(userId);
            
            // 응답 시간 계산
            Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
            Long responseTime = startTime != null ? System.currentTimeMillis() - startTime : null;
            
            // API 사용량 기록
            apiUsageService.recordApiUsage(
                    user,
                    request.getRequestURI(),
                    request.getMethod(),
                    response.getStatus(),
                    responseTime,
                    getClientIpAddress(request),
                    request.getHeader("User-Agent")
            );
            
        } catch (Exception e) {
            log.error("Failed to track API usage", e);
        }
    }
    
    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}