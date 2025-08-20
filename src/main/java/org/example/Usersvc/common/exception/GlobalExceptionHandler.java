package org.example.Usersvc.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.error.ErrorCode;
import org.example.Usersvc.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 전역 예외 처리기
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("Business exception occurred: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(e.getMessage(), errorCode.getCode()));
    }
    
    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation exception occurred: {}", e.getMessage());
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");
        
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(message, "VALIDATION_ERROR"));
    }
    
    /**
     * Bind 예외 처리
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        log.warn("Bind exception occurred: {}", e.getMessage());
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");
        
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(message, "BIND_ERROR"));
    }
    
    /**
     * 메소드 인자 타입 불일치 예외 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch exception occurred: {}", e.getMessage());
        String message = String.format("'%s' 파라미터의 값이 올바르지 않습니다.", e.getName());
        
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(message, "TYPE_MISMATCH"));
    }
    
    /**
     * HTTP 메소드 미지원 예외 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not supported exception occurred: {}", e.getMessage());
        String message = String.format("지원하지 않는 HTTP 메소드입니다: %s", e.getMethod());
        
        return ResponseEntity
                .status(405)
                .body(ApiResponse.error(message, "METHOD_NOT_SUPPORTED"));
    }
    
    /**
     * 플랜 제한 초과 예외 처리
     */
    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handlePlanLimitExceeded(PlanLimitExceededException e) {
        log.warn("Plan limit exceeded: {}", e.getMessage());
        
        return ResponseEntity
                .status(403) // HttpStatus.FORBIDDEN
                .body(ApiResponse.error(e.getMessage(), "PLAN_LIMIT_EXCEEDED"));
    }
    
    /**
     * 일반 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
        log.error("Unexpected exception occurred", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getMessage(), errorCode.getCode()));
    }
}