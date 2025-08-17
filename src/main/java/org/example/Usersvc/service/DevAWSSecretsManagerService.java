package org.example.Usersvc.service;

import org.example.Usersvc.exception.AWSSecretsManagerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 개발 환경용 Mock AWS Secrets Manager 서비스
 * 실제 AWS 없이 개발 환경에서 테스트할 수 있도록 메모리 기반 구현체 제공
 * 
 * 주요 기능:
 * - 인메모리 시크릿 저장소
 * - 가상 ARN 생성
 * - 로깅을 통한 동작 추적
 */
@Slf4j
@Service
@Primary
@Profile({"dev", "default"})
public class DevAWSSecretsManagerService implements AWSSecretsManagerService {

    // 개발 환경용 메모리 저장소
    private final Map<String, String> secretStore = new HashMap<>();
    private final Map<String, String> arnToSecretMap = new HashMap<>();

    @Override
    public String storeSecret(String secretName, String secretValue, String description) throws AWSSecretsManagerException {
        log.info("[DEV] Mock storing secret: name={}, description={}", secretName, description);
        
        // Mock ARN 생성
        String mockArn = String.format("arn:aws:secretsmanager:dev-region:123456789012:secret:%s-%s", 
                                     secretName, UUID.randomUUID().toString().substring(0, 6));
        
        // 메모리에 저장
        secretStore.put(mockArn, secretValue);
        arnToSecretMap.put(mockArn, secretName);
        
        log.info("[DEV] Mock secret stored successfully with ARN: {}", mockArn);
        return mockArn;
    }

    @Override
    public String getSecretValue(String arn) throws AWSSecretsManagerException {
        log.info("[DEV] Mock retrieving secret for ARN: {}", arn);
        
        String secretValue = secretStore.get(arn);
        if (secretValue == null) {
            throw new AWSSecretsManagerException("Secret not found for ARN: " + arn);
        }
        
        log.info("[DEV] Mock secret retrieved successfully");
        return secretValue;
    }

    @Override
    public void deleteSecret(String arn) throws AWSSecretsManagerException {
        log.info("[DEV] Mock deleting secret for ARN: {}", arn);
        
        if (!secretStore.containsKey(arn)) {
            throw new AWSSecretsManagerException("Secret not found for ARN: " + arn);
        }
        
        secretStore.remove(arn);
        arnToSecretMap.remove(arn);
        
        log.info("[DEV] Mock secret deleted successfully");
    }

    @Override
    public boolean secretExists(String arn) {
        log.info("[DEV] Mock checking if secret exists for ARN: {}", arn);
        boolean exists = secretStore.containsKey(arn);
        log.info("[DEV] Secret exists: {}", exists);
        return exists;
    }

    @Override
    public SecretMetadata getSecretMetadata(String arn) throws AWSSecretsManagerException {
        log.info("[DEV] Mock retrieving secret metadata for ARN: {}", arn);
        
        if (!secretStore.containsKey(arn)) {
            throw new AWSSecretsManagerException("Secret not found for ARN: " + arn);
        }
        
        // Mock metadata
        SecretMetadata metadata = new SecretMetadata();
        metadata.setArn(arn);
        metadata.setName(arnToSecretMap.get(arn));
        metadata.setDescription("Mock secret for development");
        
        log.info("[DEV] Mock metadata retrieved successfully");
        return metadata;
    }

    @Override
    public void updateSecretDescription(String arn, String newDescription) throws AWSSecretsManagerException {
        log.info("[DEV] Mock updating secret description for ARN: {}", arn);
        
        if (!secretStore.containsKey(arn)) {
            throw new AWSSecretsManagerException("Secret not found for ARN: " + arn);
        }
        
        log.info("[DEV] Mock secret description updated successfully");
    }
}