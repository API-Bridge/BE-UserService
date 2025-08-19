package org.example.Usersvc.util;

import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 사용자 ID 생성 유틸리티
 * 
 * 일관된 포맷으로 사용자 ID를 생성합니다.
 * 기존 데이터와의 호환성을 위해 user-XXX 형태를 유지하면서,
 * 충돌을 방지하는 안전한 ID 생성을 지원합니다.
 * 
 * 생성 포맷: user-{timestamp}-{sequence}-{random}
 * 예: user-20240101-001-a7b2c
 */
@Slf4j
public class UserIdGenerator {

    private static final String USER_PREFIX = "user-";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final AtomicLong SEQUENCE = new AtomicLong(1);

    /**
     * 새로운 사용자 ID 생성
     * 
     * 포맷: user-{YYYYMMDD}-{sequence}-{random}
     * - YYYYMMDD: 생성 날짜
     * - sequence: 일별 순번 (001부터 시작)
     * - random: 5자리 랜덤 문자열 (충돌 방지)
     * 
     * @return 생성된 사용자 ID
     */
    public static String generateUserId() {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        String sequencePart = String.format("%03d", SEQUENCE.getAndIncrement() % 1000);
        String randomPart = generateRandomString(5);
        
        String userId = USER_PREFIX + datePart + "-" + sequencePart + "-" + randomPart;
        
        log.debug("새 사용자 ID 생성됨: {}", userId);
        return userId;
    }

    /**
     * 개발용 간단한 사용자 ID 생성
     * 
     * 포맷: user-dev-{sequence}
     * 개발 환경에서 간단한 ID가 필요한 경우 사용
     * 
     * @return 개발용 사용자 ID
     */
    public static String generateDevUserId() {
        String userId = USER_PREFIX + "dev-" + String.format("%03d", SEQUENCE.getAndIncrement() % 1000);
        log.debug("개발용 사용자 ID 생성됨: {}", userId);
        return userId;
    }

    /**
     * 사용자 ID 유효성 검증
     * 
     * @param userId 검증할 사용자 ID
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public static boolean isValidUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        
        // user-로 시작하는지 확인
        if (!userId.startsWith(USER_PREFIX)) {
            return false;
        }
        
        // 최소 길이 확인 (user-XXX 최소 8자)
        if (userId.length() < 8) {
            return false;
        }
        
        // 기존 포맷들도 허용
        // 1. user-001, user-002... (기존 데이터)
        // 2. user-20240101-001-a7b2c (새 포맷)
        // 3. user-dev-001 (개발용)
        String suffix = userId.substring(USER_PREFIX.length());
        
        // 기존 숫자 포맷 (user-001)
        if (suffix.matches("^[0-9]+$")) {
            return true;
        }
        
        // 개발용 포맷 (user-dev-001)
        if (suffix.matches("^dev-[0-9]+$")) {
            return true;
        }
        
        // 새 포맷 (user-20240101-001-a7b2c)
        if (suffix.matches("^[0-9]{8}-[0-9]{3}-[a-z0-9]{5}$")) {
            return true;
        }
        
        return false;
    }

    /**
     * 사용자 ID가 기존 포맷인지 확인
     * 
     * @param userId 확인할 사용자 ID
     * @return 기존 포맷이면 true
     */
    public static boolean isLegacyFormat(String userId) {
        if (!isValidUserId(userId)) {
            return false;
        }
        
        String suffix = userId.substring(USER_PREFIX.length());
        return suffix.matches("^[0-9]+$");
    }

    /**
     * 랜덤 문자열 생성
     * 
     * @param length 생성할 문자열 길이
     * @return 생성된 랜덤 문자열
     */
    private static String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            result.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        
        return result.toString();
    }

    /**
     * 시퀀스 초기화 (테스트용)
     */
    public static void resetSequence() {
        SEQUENCE.set(1);
        log.debug("사용자 ID 시퀀스 초기화됨");
    }

    /**
     * 현재 시퀀스 번호 조회 (모니터링용)
     */
    public static long getCurrentSequence() {
        return SEQUENCE.get();
    }
}