package org.example.Usersvc.common.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 액션 로거
 * 
 * 사용자의 주요 액션들을 추적하고 로깅합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionLogger {

    private final SecurityAuditLogger securityAuditLogger;

    /**
     * 사용자 생성 액션 로그
     */
    public void logUserCreation(String userId, String auth0Id, String email) {
        String ipAddress = getCurrentIpAddress();
        String userAgent = getCurrentUserAgent();
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("userId", userId);
        actionData.put("auth0Id", auth0Id);
        actionData.put("email", maskEmail(email));
        actionData.put("registrationMethod", "AUTH0");
        
        logUserAction("USER_CREATED", userId, ipAddress, userAgent, actionData);
        
        // 보안 로그도 함께 기록
        securityAuditLogger.logAuthenticationSuccess(userId, ipAddress, userAgent);
    }

    /**
     * 사용자 로그인 액션 로그
     */
    public void logUserLogin(String userId) {
        String ipAddress = getCurrentIpAddress();
        String userAgent = getCurrentUserAgent();
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("loginType", "AUTH0_SSO");
        actionData.put("sessionStarted", true);
        
        logUserAction("USER_LOGIN", userId, ipAddress, userAgent, actionData);
        
        // 보안 로그도 함께 기록
        securityAuditLogger.logLoginSuccess(userId, ipAddress, userAgent);
    }

    /**
     * 사용자 로그아웃 액션 로그
     */
    public void logUserLogout(String userId, LocalDateTime loginTime) {
        String ipAddress = getCurrentIpAddress();
        String sessionDuration = calculateSessionDuration(loginTime);
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("sessionDuration", sessionDuration);
        actionData.put("logoutType", "NORMAL");
        
        logUserAction("USER_LOGOUT", userId, ipAddress, null, actionData);
        
        // 보안 로그도 함께 기록
        securityAuditLogger.logLogout(userId, ipAddress, sessionDuration);
    }

    /**
     * API 키 등록 액션 로그
     */
    public void logApiKeyRegistration(String userId, String secretName, boolean testSuccess) {
        String ipAddress = getCurrentIpAddress();
        String userAgent = getCurrentUserAgent();
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("secretName", secretName);
        actionData.put("keyType", "AI_API_KEY");
        actionData.put("testResult", testSuccess ? "SUCCESS" : "FAILED");
        actionData.put("awsSecretsManager", true);
        
        logUserAction("API_KEY_REGISTERED", userId, ipAddress, userAgent, actionData);
        
        // 보안 로그도 함께 기록
        securityAuditLogger.logApiKeyRegistrationSuccess(userId, secretName, ipAddress, testSuccess);
    }

    /**
     * API 키 삭제 액션 로그
     */
    public void logApiKeyDeletion(String userId, String secretName, boolean deletionSuccess) {
        String ipAddress = getCurrentIpAddress();
        String userAgent = getCurrentUserAgent();
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("secretName", secretName);
        actionData.put("keyType", "AI_API_KEY");
        actionData.put("deletionConfirmed", deletionSuccess);
        actionData.put("deletionResult", deletionSuccess ? "SUCCESS" : "FAILED");
        
        logUserAction("API_KEY_DELETED", userId, ipAddress, userAgent, actionData);
        
        // 보안 로그도 함께 기록
        securityAuditLogger.logApiKeyDeletion(userId, secretName, ipAddress);
    }

    /**
     * 구독 생성 액션 로그
     */
    public void logSubscriptionCreation(String userId, String planName, String paymentMethod, Double amount) {
        String ipAddress = getCurrentIpAddress();
        String userAgent = getCurrentUserAgent();
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("planName", planName);
        actionData.put("paymentMethod", paymentMethod);
        actionData.put("amount", amount);
        actionData.put("currency", "USD");
        actionData.put("subscriptionType", "STRIPE");
        
        logUserAction("SUBSCRIPTION_CREATED", userId, ipAddress, userAgent, actionData);
        
        // 보안 로그도 함께 기록
        securityAuditLogger.logSubscriptionChange(userId, planName, paymentMethod, amount, "USD", ipAddress);
    }

    /**
     * 구독 취소 액션 로그
     */
    public void logSubscriptionCancellation(String userId, String planName, String reason) {
        String ipAddress = getCurrentIpAddress();
        String userAgent = getCurrentUserAgent();
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("previousPlanName", planName);
        actionData.put("newPlanName", "FREE");
        actionData.put("cancellationReason", reason);
        actionData.put("autoDowngrade", true);
        
        logUserAction("SUBSCRIPTION_CANCELLED", userId, ipAddress, userAgent, actionData);
    }

    /**
     * 커스텀 API 생성 액션 로그
     */
    public void logCustomApiCreation(String userId, String apiName, int currentCount, int maxAllowed) {
        String ipAddress = getCurrentIpAddress();
        String userAgent = getCurrentUserAgent();
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("apiName", apiName);
        actionData.put("currentApiCount", currentCount);
        actionData.put("maxAllowedCount", maxAllowed);
        actionData.put("remainingSlots", maxAllowed - currentCount);
        
        logUserAction("CUSTOM_API_CREATED", userId, ipAddress, userAgent, actionData);
    }

    /**
     * API 공유 액션 로그
     */
    public void logApiSharing(String userId, String apiName, String sharedApiId) {
        String ipAddress = getCurrentIpAddress();
        String userAgent = getCurrentUserAgent();
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("originalApiName", apiName);
        actionData.put("sharedApiId", sharedApiId);
        actionData.put("sharingPlatform", "COMMUNITY");
        actionData.put("isPublic", true);
        
        logUserAction("API_SHARED", userId, ipAddress, userAgent, actionData);
    }

    /**
     * 플랜 제한 초과 시도 로그
     */
    public void logPlanLimitExceeded(String userId, String limitType, int currentUsage, int maxAllowed) {
        String ipAddress = getCurrentIpAddress();
        String userAgent = getCurrentUserAgent();
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("limitType", limitType);
        actionData.put("currentUsage", currentUsage);
        actionData.put("maxAllowed", maxAllowed);
        actionData.put("upgradeRecommended", true);
        
        logUserAction("PLAN_LIMIT_EXCEEDED", userId, ipAddress, userAgent, actionData);
    }

    /**
     * 기본 사용자 액션 로깅
     */
    private void logUserAction(String actionType, String userId, String ipAddress, 
                              String userAgent, Map<String, Object> actionData) {
        try {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("timestamp", LocalDateTime.now().toString());
            logEntry.put("actionType", actionType);
            logEntry.put("userId", userId);
            logEntry.put("ipAddress", maskIpAddress(ipAddress));
            logEntry.put("userAgent", maskUserAgent(userAgent));
            logEntry.put("service", "user-service");
            logEntry.putAll(actionData);
            
            // 구조화된 로깅
            log.info("USER_ACTION: actionType={}, userId={}, ipAddress={}, details={}", 
                    actionType, userId, maskIpAddress(ipAddress), actionData);
                    
        } catch (Exception e) {
            log.error("사용자 액션 로깅 실패: actionType={}, userId={}, error={}", 
                    actionType, userId, e.getMessage());
        }
    }

    /**
     * 현재 요청의 IP 주소 추출
     */
    private String getCurrentIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                
                // X-Forwarded-For 헤더 확인 (프록시 환경)
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                
                // X-Real-IP 헤더 확인
                String xRealIP = request.getHeader("X-Real-IP");
                if (xRealIP != null && !xRealIP.isEmpty()) {
                    return xRealIP;
                }
                
                // 기본 remote address
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("IP 주소 추출 실패: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * 현재 요청의 User-Agent 추출
     */
    private String getCurrentUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("User-Agent 추출 실패: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * 세션 지속 시간 계산
     */
    private String calculateSessionDuration(LocalDateTime loginTime) {
        if (loginTime == null) {
            return "unknown";
        }
        
        try {
            LocalDateTime now = LocalDateTime.now();
            long minutes = java.time.Duration.between(loginTime, now).toMinutes();
            
            if (minutes < 1) {
                return "< 1분";
            } else if (minutes < 60) {
                return minutes + "분";
            } else {
                long hours = minutes / 60;
                long remainingMinutes = minutes % 60;
                return hours + "시간 " + remainingMinutes + "분";
            }
        } catch (Exception e) {
            return "calculation_error";
        }
    }

    /**
     * 이메일 마스킹
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "unknown";
        }
        
        int atIndex = email.indexOf("@");
        if (atIndex > 0) {
            String localPart = email.substring(0, atIndex);
            String domainPart = email.substring(atIndex);
            
            if (localPart.length() <= 2) {
                return "**" + domainPart;
            } else {
                return localPart.substring(0, 2) + "****" + domainPart;
            }
        }
        
        return "masked_email";
    }

    /**
     * IP 주소 마스킹
     */
    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return "unknown";
        }
        
        if (ipAddress.contains(".")) {
            String[] parts = ipAddress.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + "." + parts[2] + ".xxx";
            }
        }
        
        return "masked";
    }

    /**
     * User Agent 마스킹
     */
    private String maskUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "unknown";
        }
        
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("Edge")) return "Edge";
        
        return "other";
    }

    /**
     * 사용자 삭제 액션 로그
     */
    public void logUserDeletion(String userId, String auth0Id, String reason) {
        String ipAddress = getCurrentIpAddress();
        String userAgent = getCurrentUserAgent();
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("auth0Id", auth0Id);
        actionData.put("deletionReason", reason != null ? reason : "User requested");
        actionData.put("gdprCompliant", true);
        actionData.put("dataRetention", "IMMEDIATE_DELETION");
        
        logUserAction("USER_DELETED", userId, ipAddress, userAgent, actionData);
    }

    /**
     * 중요 액션 로그 (정적 메서드)
     */
    public static void logCriticalAction(String userId, String actionType, Map<String, String> details) {
        try {
            log.warn("CRITICAL_ACTION: userId={}, actionType={}, details={}", 
                    userId, actionType, details);
        } catch (Exception e) {
            log.error("중요 액션 로깅 실패: userId={}, actionType={}, error={}", 
                    userId, actionType, e.getMessage());
        }
    }
}