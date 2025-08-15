package org.example.Usersvc.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiResponse 테스트
 */
class ApiResponseTest {
    
    @Test
    @DisplayName("데이터와 메시지를 포함한 성공 응답을 생성해야 한다")
    void shouldCreateSuccessResponseWithDataAndMessage() {
        // Given
        String testData = "test data";
        String message = "성공했습니다";
        
        // When
        ApiResponse<String> response = ApiResponse.success(testData, message);
        
        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(testData);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getErrorCode()).isNull();
    }
    
    @Test
    @DisplayName("데이터만 포함한 성공 응답을 생성해야 한다")
    void shouldCreateSuccessResponseWithDataOnly() {
        // Given
        String testData = "test data";
        
        // When
        ApiResponse<String> response = ApiResponse.success(testData);
        
        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(testData);
        assertThat(response.getMessage()).isEqualTo("요청이 성공적으로 처리되었습니다.");
        assertThat(response.getErrorCode()).isNull();
    }
    
    @Test
    @DisplayName("데이터 없는 성공 응답을 생성해야 한다")
    void shouldCreateSuccessResponseWithoutData() {
        // When
        ApiResponse<Void> response = ApiResponse.success();
        
        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("요청이 성공적으로 처리되었습니다.");
        assertThat(response.getErrorCode()).isNull();
    }
    
    @Test
    @DisplayName("메시지와 에러 코드를 포함한 에러 응답을 생성해야 한다")
    void shouldCreateErrorResponseWithMessageAndCode() {
        // Given
        String message = "오류가 발생했습니다";
        String errorCode = "USER001";
        
        // When
        ApiResponse<Void> response = ApiResponse.error(message, errorCode);
        
        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getErrorCode()).isEqualTo(errorCode);
    }
    
    @Test
    @DisplayName("메시지만 포함한 에러 응답을 생성해야 한다")
    void shouldCreateErrorResponseWithMessageOnly() {
        // Given
        String message = "오류가 발생했습니다";
        
        // When
        ApiResponse<Void> response = ApiResponse.error(message);
        
        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getErrorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
    }
    
    @Test
    @DisplayName("null 데이터로 성공 응답을 생성해야 한다")
    void shouldCreateSuccessResponseWithNullData() {
        // When
        ApiResponse<String> response = ApiResponse.success(null, "처리 완료");
        
        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("처리 완료");
        assertThat(response.getErrorCode()).isNull();
    }
}