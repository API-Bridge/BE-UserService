package org.example.Usersvc.common.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 보안 감사 로거
 * 
 * 보안 관련 중요 이벤트들을 구조화된 형태로 로깅합니다.
 */
@Slf4j
@Component
public class SecurityAuditLogger {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 사용자 인증 성공 로그
     */
    public void logAuthenticationSuccess(String userId, String ipAddress, String userAgent) {
        Map<String, Object> logData = createBaseLogData("USER_AUTHENTICATION_SUCCESS", userId, ipAddress);
        logData.put("userAgent", maskUserAgent(userAgent));
        
        logSecurityEvent(logData);
    }

    /**
     * 사용자 인증 실패 로그
     */
    public void logAuthenticationFailure(String attemptedUserId, String ipAddress, String userAgent, String reason) {
        Map<String, Object> logData = createBaseLogData("USER_AUTHENTICATION_FAILURE", attemptedUserId, ipAddress);
        logData.put("userAgent", maskUserAgent(userAgent));
        logData.put("failureReason", reason);
        logData.put("severity", "HIGH");
        
        logSecurityEvent(logData);
    }

    /**
     * 사용자 로그인 성공 로그
     */
    public void logLoginSuccess(String userId, String ipAddress, String userAgent) {
        Map<String, Object> logData = createBaseLogData("USER_LOGIN_SUCCESS", userId, ipAddress);
        logData.put("userAgent", maskUserAgent(userAgent));
        logData.put("sessionCreated", true);
        
        logSecurityEvent(logData);
    }

    /**
     * 사용자 로그아웃 로그
     */
    public void logLogout(String userId, String ipAddress, String sessionDuration) {
        Map<String, Object> logData = createBaseLogData("USER_LOGOUT", userId, ipAddress);
        logData.put("sessionDuration", sessionDuration);
        logData.put("logoutType", "NORMAL");
        
        logSecurityEvent(logData);
    }

    /**
     * AI API 키 등록 성공 로그
     */
    public void logApiKeyRegistrationSuccess(String userId, String secretName, String ipAddress, boolean testResult) {
        Map<String, Object> logData = createBaseLogData("API_KEY_REGISTRATION_SUCCESS", userId, ipAddress);
        logData.put("secretName", secretName);
        logData.put("testResult", testResult ? "SUCCESS" : "FAILED");
        logData.put("keyType", "AI_API_KEY");
        
        logSecurityEvent(logData);
    }

    /**
     * AI API 키 등록 실패 로그
     */
    public void logApiKeyRegistrationFailure(String userId, String secretName, String ipAddress, String reason) {
        Map<String, Object> logData = createBaseLogData("API_KEY_REGISTRATION_FAILURE", userId, ipAddress);
        logData.put("secretName", secretName);
        logData.put("failureReason", reason);
        logData.put("severity", "MEDIUM");
        logData.put("keyType", "AI_API_KEY");
        
        logSecurityEvent(logData);
    }

    /**
     * AI API 키 삭제 로그
     */
    public void logApiKeyDeletion(String userId, String secretName, String ipAddress) {
        Map<String, Object> logData = createBaseLogData("API_KEY_DELETION", userId, ipAddress);
        logData.put("secretName", secretName);
        logData.put("keyType", "AI_API_KEY");
        logData.put("deletionConfirmed", true);
        
        logSecurityEvent(logData);
    }

    /**
     * AI API 키 삭제 실패 로그
     */
    public void logApiKeyDeletionFailure(String userId, String arnId, String ipAddress, String reason) {
        Map<String, Object> logData = createBaseLogData("API_KEY_DELETION_FAILURE", userId, ipAddress);
        logData.put("arnId", arnId);
        logData.put("failureReason", reason);
        logData.put("severity", "MEDIUM");
        logData.put("keyType", "AI_API_KEY");
        
        logSecurityEvent(logData);
    }

    /**
     * 구독 정보 변경 로그
     */
    public void logSubscriptionChange(String userId, String planName, String paymentMethod, 
                                    Double amount, String currency, String ipAddress) {
        Map<String, Object> logData = createBaseLogData("SUBSCRIPTION_CHANGE", userId, ipAddress);
        logData.put("planName", planName);
        logData.put("paymentMethod", maskPaymentMethod(paymentMethod));
        logData.put("amount", amount);
        logData.put("currency", currency);
        logData.put("transactionType", "SUBSCRIPTION");
        
        logSecurityEvent(logData);
    }

    /**
     * 결제 정보 로그
     */
    public void logPaymentTransaction(String userId, String paymentMethod, Double amount, 
                                    String currency, String transactionId, String status, String ipAddress) {
        Map<String, Object> logData = createBaseLogData("PAYMENT_TRANSACTION", userId, ipAddress);
        logData.put("paymentMethod", maskPaymentMethod(paymentMethod));
        logData.put("amount", amount);
        logData.put("currency", currency);
        logData.put("transactionId", transactionId);
        logData.put("status", status);
        logData.put("severity", "HIGH");
        
        logSecurityEvent(logData);
    }

    /**
     * 권한 없는 접근 시도 로그
     */
    public void logUnauthorizedAccess(String userId, String resource, String action, String ipAddress) {
        Map<String, Object> logData = createBaseLogData("UNAUTHORIZED_ACCESS_ATTEMPT", userId, ipAddress);
        logData.put("attemptedResource", resource);
        logData.put("attemptedAction", action);
        logData.put("severity", "HIGH");
        logData.put("blocked", true);
        
        logSecurityEvent(logData);
    }

    /**
     * 비정상적인 API 호출 패턴 로그
     */
    public void logSuspiciousApiActivity(String userId, String endpoint, int requestCount, 
                                       String timeWindow, String ipAddress) {
        Map<String, Object> logData = createBaseLogData("SUSPICIOUS_API_ACTIVITY", userId, ipAddress);
        logData.put("endpoint", endpoint);
        logData.put("requestCount", requestCount);
        logData.put("timeWindow", timeWindow);
        logData.put("severity", "MEDIUM");
        logData.put("possibleAttack", true);
        
        logSecurityEvent(logData);
    }

    /**
     * 기본 로그 데이터 생성
     */
    private Map<String, Object> createBaseLogData(String eventType, String userId, String ipAddress) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", LocalDateTime.now().toString());
        logData.put("eventType", eventType);
        logData.put("userId", userId);
        logData.put("ipAddress", maskIpAddress(ipAddress));
        logData.put("service", "user-service");
        logData.put("version", "1.0");
        return logData;
    }

    /**
     * 보안 이벤트 로깅 (구조화된 JSON 형태)
     */
    private void logSecurityEvent(Map<String, Object> logData) {
        try {
            String jsonLog = objectMapper.writeValueAsString(logData);
            log.info("SECURITY_AUDIT: {}", jsonLog);
        } catch (JsonProcessingException e) {
            log.error("보안 로그 직렬화 실패: {}", e.getMessage());
            log.info("SECURITY_AUDIT: eventType={}, userId={}, timestamp={}", 
                    logData.get("eventType"), logData.get("userId"), logData.get("timestamp"));
        }
    }

    /**
     * IP 주소 마스킹 (개인정보 보호)
     */
    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return "unknown";
        }
        
        // IPv4 마스킹 (예: 192.168.1.100 -> 192.168.1.xxx)
        if (ipAddress.contains(".")) {
            String[] parts = ipAddress.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + "." + parts[2] + ".xxx";
            }
        }
        
        return "masked";
    }

    /**
     * 결제수단 마스킹
     */
    private String maskPaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            return "unknown";
        }
        
        // 카드 번호인 경우 마스킹 (예: 4242424242424242 -> ****-****-****-4242)
        if (paymentMethod.matches("\\d{16}")) {
            return "****-****-****-" + paymentMethod.substring(12);
        }
        
        // 기타 결제수단은 타입만 기록
        return paymentMethod.length() > 4 ? paymentMethod.substring(0, 4) + "****" : "****";
    }

    /**
     * User Agent 마스킹
     */
    private String maskUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "unknown";
        }
        
        // 브라우저 정보만 추출 (개인정보 제거)
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("Edge")) return "Edge";
        
        return "other";
    }
}