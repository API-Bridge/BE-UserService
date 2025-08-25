package org.example.Usersvc.common.constants;

/**
 * 에러 관련 상수 정의
 * 소나큐브 코드 품질을 위한 하드코딩된 문자열 상수화
 */
public final class ErrorConstants {
    
    private ErrorConstants() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // HTTP 에러 코드
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String UNAUTHORIZED_ACCESS = "UNAUTHORIZED_ACCESS";
    public static final String FORBIDDEN_ACCESS = "FORBIDDEN_ACCESS";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    
    // 비즈니스 로직 에러
    public static final String PLAN_LIMIT_EXCEEDED = "PLAN_LIMIT_EXCEEDED";
    public static final String SUBSCRIPTION_ERROR = "SUBSCRIPTION_ERROR";
    public static final String PAYMENT_ERROR = "PAYMENT_ERROR";
    public static final String TOSSPAY_ERROR = "TOSSPAY_ERROR";
    
    // 에러 메시지
    public static final String USER_CREATION_FAILED = "사용자 생성에 실패했습니다.";
    public static final String USER_RETRIEVAL_FAILED = "사용자 조회에 실패했습니다.";
    public static final String SUBSCRIPTION_OPERATION_FAILED = "구독 처리에 실패했습니다.";
    public static final String PAYMENT_PROCESSING_FAILED = "결제 처리에 실패했습니다.";
    public static final String INTERNAL_SERVER_ERROR_MSG = "서버 내부 오류가 발생했습니다.";
    public static final String INVALID_INPUT_DATA = "입력 데이터가 올바르지 않습니다.";
    public static final String RESOURCE_ACCESS_DENIED = "리소스에 대한 접근이 거부되었습니다.";
    
    // 데이터베이스 관련
    public static final String DATABASE_OPERATION_FAILED = "데이터베이스 작업이 실패했습니다.";
    public static final String DATA_INTEGRITY_VIOLATION = "데이터 무결성 위반이 발생했습니다.";
    
    // 외부 서비스 관련
    public static final String EXTERNAL_SERVICE_ERROR = "외부 서비스 호출에 실패했습니다.";
    public static final String API_RATE_LIMIT_EXCEEDED = "API 호출 한도를 초과했습니다.";
}