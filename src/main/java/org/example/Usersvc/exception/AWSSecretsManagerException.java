package org.example.Usersvc.exception;

/**
 * AWS Secrets Manager 연동 중 발생하는 예외
 * 
 * AWS Secrets Manager API 호출 과정에서 발생할 수 있는
 * 다양한 오류 상황을 나타내는 예외 클래스입니다.
 * 
 * 주요 발생 시나리오:
 * - AWS API 호출 실패
 * - 권한 부족으로 인한 접근 거부
 * - 시크릿 이름 중복
 * - 시크릿을 찾을 수 없음
 * - 네트워크 연결 문제
 * - AWS 서비스 제한 초과
 */
public class AWSSecretsManagerException extends RuntimeException {
    
    /**
     * 기본 생성자
     * 
     * @param message 예외 메시지
     */
    public AWSSecretsManagerException(String message) {
        super(message);
    }
    
    /**
     * 원인 예외를 포함하는 생성자
     * 
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public AWSSecretsManagerException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 원인 예외만을 포함하는 생성자
     * 
     * @param cause 원인 예외
     */
    public AWSSecretsManagerException(Throwable cause) {
        super(cause);
    }
}