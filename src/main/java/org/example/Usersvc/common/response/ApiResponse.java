package org.example.Usersvc.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모든 API 응답에 사용되는 표준 응답 포맷
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    
    private ApiResponse(boolean success, String message, T data, String errorCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
    }
    
    /**
     * 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, null);
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "요청이 성공적으로 처리되었습니다.");
    }
    
    public static ApiResponse<Void> success() {
        return success(null, "요청이 성공적으로 처리되었습니다.");
    }
    
    /**
     * 실패 응답 생성
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, message, null, errorCode);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return error(message, "INTERNAL_SERVER_ERROR");
    }
}