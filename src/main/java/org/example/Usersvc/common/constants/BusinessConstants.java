package org.example.Usersvc.common.constants;

/**
 * 비즈니스 로직 관련 상수 정의
 * 소나큐브 코드 품질을 위한 매직 넘버 상수화
 */
public final class BusinessConstants {
    
    private BusinessConstants() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // 플랜 관련 상수
    public static final String FREE_PLAN = "FREE";
    public static final String PRO_PLAN = "PRO";
    
    // 페이징 기본값
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MIN_PAGE_SIZE = 1;
    
    // API 제한
    public static final int FREE_API_LIMIT_PER_MINUTE = 10;
    public static final int FREE_API_LIMIT_PER_HOUR = 100;
    public static final int FREE_API_LIMIT_PER_DAY = 1000;
    public static final int FREE_API_LIMIT_PER_MONTH = 100;
    
    public static final int PRO_API_LIMIT_PER_MINUTE = 60;
    public static final int PRO_API_LIMIT_PER_HOUR = 3600;
    public static final int PRO_API_LIMIT_PER_DAY = 86400;
    public static final int PRO_API_LIMIT_PER_MONTH = 10000;
    
    // 커스텀 API/공유 API 제한
    public static final int FREE_MAX_CUSTOM_APIS = 5;
    public static final int FREE_MAX_SHARED_APIS = 3;
    public static final int FREE_MAX_SECRETS_ARN = 3;
    
    public static final int PRO_MAX_CUSTOM_APIS = 50;
    public static final int PRO_MAX_SHARED_APIS = 20;
    public static final int PRO_MAX_SECRETS_ARN = 20;
    
    // 결제 관련
    public static final double FREE_PLAN_PRICE = 0.00;
    public static final double PRO_PLAN_PRICE = 22.00;
    
    // 웹훅 재시도 설정
    public static final int MAX_WEBHOOK_RETRY_COUNT = 3;
    public static final long WEBHOOK_RETRY_DELAY_MS = 1000L;
    
    // 사용자 ID 생성
    public static final String USER_ID_PREFIX = "user-";
    public static final int USER_ID_MIN_LENGTH = 8;
    public static final int USER_ID_RANDOM_LENGTH = 6;
    
    // HTTP 상태 코드
    public static final int HTTP_STATUS_OK = 200;
    public static final int HTTP_STATUS_CREATED = 201;
    public static final int HTTP_STATUS_BAD_REQUEST = 400;
    public static final int HTTP_STATUS_UNAUTHORIZED = 401;
    public static final int HTTP_STATUS_FORBIDDEN = 403;
    public static final int HTTP_STATUS_NOT_FOUND = 404;
    public static final int HTTP_STATUS_INTERNAL_SERVER_ERROR = 500;
    
    // 로깅 관련
    public static final String LOG_PREFIX_ADMIN = "[ADMIN]";
    public static final String LOG_PREFIX_USER = "[USER]";
    public static final String LOG_PREFIX_PAYMENT = "[PAYMENT]";
    public static final String LOG_PREFIX_API = "[API]";
    
    // 디폴트 응답 메시지
    public static final String SUCCESS_MESSAGE = "요청이 성공적으로 처리되었습니다.";
    public static final String CREATED_MESSAGE = "리소스가 성공적으로 생성되었습니다.";
    public static final String UPDATED_MESSAGE = "리소스가 성공적으로 업데이트되었습니다.";
    public static final String DELETED_MESSAGE = "리소스가 성공적으로 삭제되었습니다.";
    
    // 정규식 패턴
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
    public static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    public static final String ARN_REGEX = "^arn:aws:[a-zA-Z0-9-]+:[a-zA-Z0-9-]*:[0-9]*:[a-zA-Z0-9-/]*$";
    
    // 시간 관련 상수
    public static final long ONE_MINUTE_IN_SECONDS = 60L;
    public static final long ONE_HOUR_IN_SECONDS = 3600L;
    public static final long ONE_DAY_IN_SECONDS = 86400L;
    public static final long ONE_MONTH_IN_SECONDS = 2592000L; // 30 days
}