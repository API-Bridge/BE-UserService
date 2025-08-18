package org.example.Usersvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.service.SubscriptionPlanManagementService;
import org.example.Usersvc.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionInterceptor implements HandlerInterceptor {

    private final UserService userService;
    private final SubscriptionPlanManagementService planManagementService;
    
    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 개발 환경 및 신뢰 기반 모드에서는 구독 체크 비활성화
        if ("default".equals(activeProfile) || "dev".equals(activeProfile) || "trusted-gateway".equals(activeProfile)) {
            log.debug("개발/신뢰 기반 환경에서 구독 체크를 건너뜁니다 - Profile: {}", activeProfile);
            return true;
        }
        
        // HandlerMethod가 아닌 경우 (정적 리소스 등) 통과
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String userId = extractUserId(request);
        if (userId == null) {
            log.warn("사용자 ID가 없는 요청 - URL: {}", request.getRequestURL());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "사용자 인증이 필요합니다.");
            return false;
        }

        Optional<User> userOptional = userService.getUserById(userId);
        if (userOptional.isEmpty()) {
            log.warn("존재하지 않는 사용자 ID - userId: {}", userId);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 사용자입니다.");
            return false;
        }
        
        User user = userOptional.get();

        UserSubscription subscription = planManagementService.getCurrentSubscription(user);
        
        // 구독 만료 확인
        if (subscription.isExpired()) {
            log.warn("만료된 구독 - userId: {}, isActive: {}", userId, subscription.getIsActive());
            sendErrorResponse(response, HttpServletResponse.SC_PAYMENT_REQUIRED, "구독이 만료되었습니다. 구독을 갱신해주세요.");
            return false;
        }

        // Rate Limit 확인
        if (!planManagementService.checkRateLimit(user)) {
            log.warn("Rate Limit 초과 - userId: {}, plan: {}", userId, subscription.getPlan().getPlanName());
            sendErrorResponse(response, 429, "API 호출 제한을 초과했습니다. 잠시 후 다시 시도해주세요.");
            return false;
        }

        // API 사용량 제한 확인
        String action = determineAction(request);
        if (!planManagementService.checkApiUsageLimit(user, action)) {
            log.warn("API 사용량 제한 초과 - userId: {}, action: {}, plan: {}", 
                    userId, action, subscription.getPlan().getPlanName());
            sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, 
                    "플랜의 API 사용량 한도를 초과했습니다. 플랜을 업그레이드해주세요.");
            return false;
        }

        // 사용량 기록
        planManagementService.recordApiUsage(user, action);
        
        log.debug("API 요청 허용 - userId: {}, action: {}, plan: {}", 
                userId, action, subscription.getPlan().getPlanName());
        
        return true;
    }

    private String extractUserId(HttpServletRequest request) {
        // HTTP 헤더에서 사용자 ID 추출 (기존 방식 - 하위 호환성)
        String userId = request.getHeader("User-ID");
        if (userId != null) {
            return userId;
        }

        // API Gateway에서 전송하는 X-User-Subject 헤더 확인
        String userSubject = request.getHeader("X-User-Subject");
        if (userSubject != null) {
            return userSubject;
        }

        // Authorization 헤더에서 JWT 토큰 파싱
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                // Spring Security Context에서 JWT 토큰 정보 추출
                org.springframework.security.core.Authentication authentication = 
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) {
                    org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtToken = 
                        (org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) authentication;
                    return jwtToken.getToken().getSubject();
                }
            } catch (Exception e) {
                log.debug("JWT 토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
            }
        }

        return null;
    }

    private String determineAction(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // API 엔드포인트별 액션 결정
        if (uri.contains("/api/custom") && "POST".equals(method)) {
            return "CREATE_API";
        } else if (uri.contains("/api/custom") && "PUT".equals(method)) {
            return "UPDATE_API";
        } else if (uri.contains("/api/custom") && "DELETE".equals(method)) {
            return "DELETE_API";
        } else if (uri.contains("/api/share") && "POST".equals(method)) {
            return "SHARE_API";
        } else if (uri.contains("/api/usage")) {
            return "VIEW_USAGE";
        }

        return "API_CALL";
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format(
                "{\"success\": false, \"message\": \"%s\", \"timestamp\": \"%s\"}", 
                message, 
                java.time.LocalDateTime.now().toString()
        );
        
        response.getWriter().write(jsonResponse);
    }
}