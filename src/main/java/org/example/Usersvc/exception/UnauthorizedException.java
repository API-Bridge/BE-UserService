package org.example.Usersvc.exception;

/**
 * 인증되지 않은 접근 시 발생하는 예외
 * 
 * HTTP 401 Unauthorized 상태와 매핑됩니다.
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}