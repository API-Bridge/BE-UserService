package org.example.Usersvc.service;

import org.example.Usersvc.exception.AWSSecretsManagerException;

/**
 * AWS Secrets Manager 연동 서비스 인터페이스
 * 
 * AWS Secrets Manager와의 연동을 담당하는 서비스 인터페이스입니다.
 * BYOK(Bring Your Own Key) 기능 구현을 위해 사용자의 암호화 키를
 * AWS Secrets Manager에 안전하게 저장하고 관리하는 기능을 제공합니다.
 * 
 * 주요 기능:
 * - 사용자 제공 암호화 키를 AWS Secrets Manager에 저장
 * - 저장된 시크릿의 ARN 반환
 * - ARN을 통한 시크릿 값 조회
 * - 시크릿 삭제 및 리소스 정리
 * - AWS API 에러 처리 및 재시도 로직
 * 
 * 보안 고려사항:
 * - 모든 시크릿은 AWS KMS를 통해 암호화됨
 * - 접근 권한은 IAM 정책을 통해 제어
 * - 감사 로그는 CloudTrail을 통해 기록
 * - 시크릿 값은 메모리에서 즉시 제거
 */
public interface AWSSecretsManagerService {
    
    /**
     * 사용자 제공 시크릿을 AWS Secrets Manager에 저장
     * 
     * 사용자가 제공한 암호화 키나 시크릿 정보를
     * AWS Secrets Manager에 안전하게 저장하고,
     * 생성된 시크릿의 ARN을 반환합니다.
     * 
     * @param secretName 시크릿 이름 (고유해야 함)
     * @param secretValue 저장할 시크릿 값
     * @param description 시크릿 설명 (선택적)
     * @return 생성된 시크릿의 ARN
     * @throws AWSSecretsManagerException AWS API 호출 실패 시
     * @throws IllegalArgumentException 입력 파라미터가 유효하지 않은 경우
     */
    String storeSecret(String secretName, String secretValue, String description);
    
    /**
     * ARN을 통해 시크릿 값 조회
     * 
     * 저장된 시크릿의 ARN을 사용하여
     * AWS Secrets Manager에서 실제 시크릿 값을 조회합니다.
     * 
     * @param arn 조회할 시크릿의 ARN
     * @return 시크릿 값
     * @throws AWSSecretsManagerException AWS API 호출 실패 시
     * @throws IllegalArgumentException ARN이 유효하지 않은 경우
     */
    String getSecretValue(String arn);
    
    /**
     * ARN을 통해 시크릿 삭제
     * 
     * 지정된 ARN의 시크릿을 AWS Secrets Manager에서 삭제합니다.
     * 삭제는 즉시 수행되거나 복구 기간을 둘 수 있습니다.
     * 
     * @param arn 삭제할 시크릿의 ARN
     * @throws AWSSecretsManagerException AWS API 호출 실패 시
     * @throws IllegalArgumentException ARN이 유효하지 않은 경우
     */
    void deleteSecret(String arn);
    
    /**
     * 시크릿 존재 여부 확인
     * 
     * 지정된 ARN의 시크릿이 AWS Secrets Manager에 존재하는지 확인합니다.
     * 
     * @param arn 확인할 시크릿의 ARN
     * @return 시크릿이 존재하면 true, 그렇지 않으면 false
     * @throws AWSSecretsManagerException AWS API 호출 실패 시
     */
    boolean secretExists(String arn);
    
    /**
     * 시크릿 메타데이터 조회
     * 
     * 시크릿의 값은 제외하고 메타데이터만 조회합니다.
     * 시크릿의 생성 시간, 마지막 수정 시간 등의 정보를 포함합니다.
     * 
     * @param arn 조회할 시크릿의 ARN
     * @return 시크릿 메타데이터
     * @throws AWSSecretsManagerException AWS API 호출 실패 시
     */
    SecretMetadata getSecretMetadata(String arn);
    
    /**
     * 시크릿 설명 업데이트
     * 
     * 기존 시크릿의 설명 정보를 업데이트합니다.
     * 
     * @param arn 업데이트할 시크릿의 ARN
     * @param newDescription 새로운 설명
     * @throws AWSSecretsManagerException AWS API 호출 실패 시
     */
    void updateSecretDescription(String arn, String newDescription);
}