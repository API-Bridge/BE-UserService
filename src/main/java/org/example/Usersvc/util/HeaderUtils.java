package org.example.Usersvc.util;

import lombok.extern.slf4j.Slf4j;

/**
 * HTTP 헤더 처리를 위한 유틸리티 클래스
 */
@Slf4j
public class HeaderUtils {
    
    /**
     * X-User-Id 헤더값을 정리하여 반환
     * 
     * HTTP 헤더가 중복으로 설정된 경우 쉼표로 구분된 값들 중 첫 번째 값만 사용
     * 예: "user-123,admin-user" -> "user-123"
     * 
     * @param userIdHeader 원본 X-User-Id 헤더 값
     * @return 정리된 사용자 ID
     */
    public static String extractUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            log.warn("X-User-Id 헤더가 비어있습니다");
            return null;
        }
        
        // 쉼표로 구분된 경우 첫 번째 값만 사용
        String cleanUserId = userIdHeader.contains(",") 
            ? userIdHeader.split(",")[0].trim() 
            : userIdHeader.trim();
            
        // 로깅 (원본과 처리된 값이 다른 경우에만)
        if (!userIdHeader.equals(cleanUserId)) {
            log.info("X-User-Id 헤더 정리 - 원본: '{}', 처리된 값: '{}'", userIdHeader, cleanUserId);
        }
        
        return cleanUserId;
    }
    
    /**
     * 관리자 사용자 ID인지 확인
     * 
     * @param userId 사용자 ID
     * @return 관리자인 경우 true
     */
    public static boolean isAdminUser(String userId) {
        return userId != null && userId.contains("admin");
    }
}