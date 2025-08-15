package org.example.Usersvc.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    // 사용자 관련
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER001", "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER002", "이미 존재하는 사용자입니다."),
    INVALID_USER_DATA(HttpStatus.BAD_REQUEST, "USER003", "사용자 정보가 올바르지 않습니다."),
    
    // API 사용량 관련
    API_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "API001", "API 호출 한도를 초과했습니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "API002", "요청 빈도 제한을 초과했습니다."),
    INVALID_SUBSCRIPTION_PLAN(HttpStatus.BAD_REQUEST, "API003", "유효하지 않은 구독 플랜입니다."),
    
    // 보안 관련
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "AUTH001", "인증되지 않은 접근입니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "AUTH002", "접근 권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH003", "유효하지 않은 토큰입니다."),
    
    // 시스템 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS001", "내부 서버 오류가 발생했습니다."),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "SYS002", "외부 API 호출 중 오류가 발생했습니다."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS003", "데이터베이스 오류가 발생했습니다.");
    
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}