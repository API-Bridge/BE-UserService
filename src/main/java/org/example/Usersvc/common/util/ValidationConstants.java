package org.example.Usersvc.common.util;

/**
 * 검증 관련 상수 모음 클래스
 * 
 * 매직 넘버와 하드코딩된 문자열을 제거하여 코드 품질을 향상시킵니다.
 */
public final class ValidationConstants {
    
    private ValidationConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // AWS Secrets Manager 제한사항
    public static final int MAX_SECRET_NAME_LENGTH = 512;
    public static final int MAX_SECRET_VALUE_LENGTH = 65536; // 64KB
    
    // 사용자 ID 관련
    public static final String DEFAULT_DEV_USER_ID = "user-001";
    public static final String DEV_PRO_USER_ID = "user-pro-002";
    
    // 페이징 관련
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    
    // API 제한 관련 메시지
    public static final String CUSTOM_API_LIMIT_EXCEEDED_MESSAGE = "커스텀 API 제한을 초과했습니다. 현재: %d개, 최대: %d개.";
    public static final String SHARED_API_LIMIT_EXCEEDED_MESSAGE = "공유 API 제한을 초과했습니다. 현재: %d개, 최대: %d개.";
    public static final String RATE_LIMIT_EXCEEDED_MESSAGE = "%s당 요청 제한을 초과했습니다. 현재: %d회, 최대: %d회";
    public static final String UPGRADE_RECOMMENDATION_SUFFIX = " PRO 플랜으로 업그레이드하여 더 많은 기능을 이용하세요.";
    
    // 에러 코드
    public static final String INVALID_USER_ID = "INVALID_USER_ID";
    public static final String INVALID_EMAIL = "INVALID_EMAIL";
    public static final String INVALID_AUTH0_ID = "INVALID_AUTH0_ID";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String PLAN_LIMIT_EXCEEDED = "PLAN_LIMIT_EXCEEDED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    
    // 에러 메시지
    public static final String USER_ID_REQUIRED_MESSAGE = "사용자 ID는 필수입니다.";
    public static final String EMAIL_REQUIRED_MESSAGE = "이메일은 필수입니다.";
    public static final String EMAIL_INVALID_FORMAT_MESSAGE = "올바른 이메일 형식이 아닙니다.";
    public static final String AUTH0_ID_REQUIRED_MESSAGE = "Auth0 ID는 필수입니다.";
    public static final String AUTH0_ID_INVALID_FORMAT_MESSAGE = "올바른 Auth0 ID 형식이 아닙니다.";
    public static final String SECRET_NAME_REQUIRED_MESSAGE = "시크릿 이름은 필수입니다.";
    public static final String SECRET_VALUE_REQUIRED_MESSAGE = "시크릿 값은 필수입니다.";
    public static final String SECRET_NAME_TOO_LONG_MESSAGE = "시크릿 이름이 너무 깁니다. (최대 512자)";
    public static final String SECRET_VALUE_TOO_LARGE_MESSAGE = "시크릿 값이 너무 큽니다. (최대 65KB)";
    public static final String USER_NOT_FOUND_MESSAGE = "사용자를 찾을 수 없습니다.";
    
    // Stripe 관련
    public static final String STRIPE_TEST_KEY_PREFIX = "sk_test_";
    public static final String STRIPE_LIVE_KEY_PREFIX = "sk_live_";
    public static final String STRIPE_DUMMY_KEY = "sk_test_dummy_key";
    public static final String STRIPE_MOCK_KEY = "mock_key_for_dev";
    
    // 정규식 패턴
    public static final String AUTH0_ID_PATTERN = "^(auth0|google-oauth2|github|facebook|twitter|linkedin|apple|microsoft|windowslive|oauth2)\\|[a-zA-Z0-9._-]{1,64}$";
    public static final String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    
    // 시간 관련
    public static final int SESSION_TIMEOUT_MINUTES = 30;
    public static final int JWT_EXPIRY_HOURS = 24;
    
    // 로깅 관련
    public static final String STRIPE_CONFIG_MASKED_MESSAGE = "Stripe 설정 상태: {}";
    public static final String CONFIGURED_STATUS = "설정됨";
    public static final String NOT_CONFIGURED_STATUS = "미설정";
}