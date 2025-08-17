package org.example.Usersvc.service;

import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.exception.AWSSecretsManagerException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.*;

/**
 * 운영 환경용 실제 AWS Secrets Manager 서비스
 * 실제 AWS Secrets Manager API를 사용하여 시크릿을 관리합니다.
 * 
 * 주요 기능:
 * - AWS SDK를 통한 실제 시크릿 저장
 * - 실제 ARN 반환
 * - AWS API 에러 처리
 */
@Slf4j
@Service
@Profile({"prod", "default"})
public class ProdAWSSecretsManagerService implements AWSSecretsManagerService {

    private final SecretsManagerClient secretsManagerClient;

    public ProdAWSSecretsManagerService() {
        this.secretsManagerClient = SecretsManagerClient.builder()
            .region(software.amazon.awssdk.regions.Region.US_EAST_1)
            .build();
    }

    @Override
    public String storeSecret(String secretName, String secretValue, String description) throws AWSSecretsManagerException {
        try {
            log.info("Storing secret in AWS Secrets Manager: name={}", secretName);
            
            CreateSecretRequest request = CreateSecretRequest.builder()
                .name(secretName)
                .secretString(secretValue)
                .description(description != null ? description : "User provided secret")
                .build();

            CreateSecretResponse response = secretsManagerClient.createSecret(request);
            
            log.info("Secret stored successfully with ARN: {}", response.arn());
            return response.arn();
            
        } catch (SecretsManagerException e) {
            log.error("Failed to store secret in AWS Secrets Manager: {}", e.getMessage(), e);
            throw new AWSSecretsManagerException("Failed to store secret: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while storing secret: {}", e.getMessage(), e);
            throw new AWSSecretsManagerException("Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public String getSecretValue(String arn) throws AWSSecretsManagerException {
        try {
            log.info("Retrieving secret from AWS Secrets Manager: arn={}", arn);
            
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(arn)
                .build();

            GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
            
            log.info("Secret retrieved successfully from ARN: {}", arn);
            return response.secretString();
            
        } catch (SecretsManagerException e) {
            log.error("Failed to retrieve secret from AWS Secrets Manager: {}", e.getMessage(), e);
            throw new AWSSecretsManagerException("Failed to retrieve secret: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while retrieving secret: {}", e.getMessage(), e);
            throw new AWSSecretsManagerException("Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteSecret(String arn) throws AWSSecretsManagerException {
        try {
            log.info("Deleting secret from AWS Secrets Manager: arn={}", arn);
            
            DeleteSecretRequest request = DeleteSecretRequest.builder()
                .secretId(arn)
                .forceDeleteWithoutRecovery(false) // 30일 복구 기간 유지
                .build();

            secretsManagerClient.deleteSecret(request);
            
            log.info("Secret deletion initiated for ARN: {}", arn);
            
        } catch (SecretsManagerException e) {
            log.error("Failed to delete secret from AWS Secrets Manager: {}", e.getMessage(), e);
            throw new AWSSecretsManagerException("Failed to delete secret: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while deleting secret: {}", e.getMessage(), e);
            throw new AWSSecretsManagerException("Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean secretExists(String arn) throws AWSSecretsManagerException {
        try {
            DescribeSecretRequest request = DescribeSecretRequest.builder()
                .secretId(arn)
                .build();

            secretsManagerClient.describeSecret(request);
            return true;
            
        } catch (ResourceNotFoundException e) {
            return false;
        } catch (SecretsManagerException e) {
            log.error("Failed to check secret existence: {}", e.getMessage(), e);
            throw new AWSSecretsManagerException("Failed to check secret existence: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while checking secret existence: {}", e.getMessage(), e);
            throw new AWSSecretsManagerException("Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public SecretMetadata getSecretMetadata(String arn) throws AWSSecretsManagerException {
        try {
            log.info("Retrieving secret metadata from AWS Secrets Manager: arn={}", arn);
            
            DescribeSecretRequest request = DescribeSecretRequest.builder()
                .secretId(arn)
                .build();
                
            DescribeSecretResponse response = secretsManagerClient.describeSecret(request);
            
            SecretMetadata metadata = new SecretMetadata();
            metadata.setArn(response.arn());
            metadata.setName(response.name());
            metadata.setDescription(response.description() != null ? response.description() : "");
            
            // Instant를 LocalDateTime으로 변환
            if (response.createdDate() != null) {
                metadata.setCreatedDate(java.time.LocalDateTime.ofInstant(
                    response.createdDate(), java.time.ZoneId.systemDefault()));
            }
            if (response.lastChangedDate() != null) {
                metadata.setLastChangedDate(java.time.LocalDateTime.ofInstant(
                    response.lastChangedDate(), java.time.ZoneId.systemDefault()));
            }
            if (response.lastAccessedDate() != null) {
                metadata.setLastAccessedDate(java.time.LocalDateTime.ofInstant(
                    response.lastAccessedDate(), java.time.ZoneId.systemDefault()));
            }
            
            log.info("Secret metadata retrieved successfully: {}", arn);
            return metadata;
            
        } catch (SecretsManagerException e) {
            log.error("Failed to retrieve secret metadata: {}", e.getMessage(), e);
            throw new AWSSecretsManagerException("Failed to retrieve secret metadata: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while retrieving secret metadata: {}", e.getMessage(), e);
            throw new AWSSecretsManagerException("Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateSecretDescription(String arn, String newDescription) throws AWSSecretsManagerException {
        try {
            log.info("Updating secret description in AWS Secrets Manager: arn={}", arn);
            
            UpdateSecretRequest request = UpdateSecretRequest.builder()
                .secretId(arn)
                .description(newDescription)
                .build();
                
            secretsManagerClient.updateSecret(request);
            log.info("Secret description updated successfully: {}", arn);
            
        } catch (SecretsManagerException e) {
            log.error("Failed to update secret description: {}", e.getMessage(), e);
            throw new AWSSecretsManagerException("Failed to update secret description: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while updating secret description: {}", e.getMessage(), e);
            throw new AWSSecretsManagerException("Unexpected error: " + e.getMessage(), e);
        }
    }
}
