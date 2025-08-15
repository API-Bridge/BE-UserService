package org.example.Usersvc.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.error.ErrorCode;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.service.DevRateLimitService;
import org.example.Usersvc.service.RateLimitService;
import org.example.Usersvc.service.UserService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API 호출 빈도 제한 인터셉터
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final UserService userService;
    private final ObjectMapper objectMapper;
    
    public RateLimitInterceptor(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }
    
    // 프로덕션 환경용 Rate Limit 서비스 (optional)
    @Autowired(required = false)
    private RateLimitService rateLimitService;
    
    // 개발 환경용 Rate Limit 서비스 (optional)
    @Autowired(required = false)
    private DevRateLimitService devRateLimitService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 요청은 제외
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        try {
            // 사용자 정보 추출 (현재는 헤더에서 추출, 실제로는 JWT 토큰에서 추출)
            String userIdHeader = request.getHeader("X-User-ID");
            if (userIdHeader == null) {
                log.debug("No user ID header found, skipping rate limit check");
                return true; // 인증되지 않은 요청은 통과 (다른 보안 필터에서 처리)
            }
            
            Long userId = Long.parseLong(userIdHeader);
            User user = userService.findByUserId(userId);
            
            // Rate Limit 검사
            boolean allowed = checkRateLimit(user);
            
            if (!allowed) {
                handleRateLimitExceeded(response);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error in rate limit interceptor", e);
            // 오류 시에는 기본적으로 허용 (graceful degradation)
            return true;
        }
    }
    
    /**
     * Rate Limit 검사 (환경에 따라 다른 서비스 사용)
     */
    private boolean checkRateLimit(User user) {
        try {
            // 개발 환경에서는 DevRateLimitService 사용
            if (devRateLimitService != null) {
                return devRateLimitService.isAllowedPerMinute(user) &&
                       devRateLimitService.isAllowedPerHour(user) &&
                       devRateLimitService.isAllowedPerDay(user);
            }
            
            // 프로덕션 환경에서는 RateLimitService 사용
            return rateLimitService.isAllowedPerMinute(user) &&
                   rateLimitService.isAllowedPerHour(user) &&
                   rateLimitService.isAllowedPerDay(user);
                   
        } catch (Exception e) {
            log.error("Error checking rate limit for user: {}", user.getUserId(), e);
            return true; // 오류 시 허용
        }
    }
    
    /**
     * Rate Limit 초과 시 응답 처리
     */
    private void handleRateLimitExceeded(HttpServletResponse response) throws Exception {
        response.setStatus(429); // HttpServletResponse.SC_TOO_MANY_REQUESTS는 Jakarta EE에 없음
        response.setContentType("application/json;charset=UTF-8");
        
        ErrorCode errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;
        ApiResponse<Void> apiResponse = ApiResponse.error(errorCode.getMessage(), errorCode.getCode());
        
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(jsonResponse);
        
        log.warn("Rate limit exceeded for request: {}", response);
    }
}