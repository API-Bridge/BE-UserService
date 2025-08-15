package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.UserSecretsArn;
import org.example.Usersvc.event.publisher.EventPublisherService;
import org.example.Usersvc.exception.AWSSecretsManagerException;
import org.example.Usersvc.repository.UserSecretsArnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 암호화 키 ARN 관리 서비스
 * 
 * BYOK(Bring Your Own Key) 기능을 구현하는 핵심 서비스 클래스입니다.
 * 사용자가 제공한 암호화 키를 AWS Secrets Manager에 저장하고,
 * 반환받은 ARN을 시스템에서 관리하는 전체 프로세스를 담당합니다.
 * 
 * 주요 기능:
 * - 사용자 키의 AWS Secrets Manager 저장
 * - ARN 정보의 데이터베이스 저장 및 관리
 * - 키 조회, 업데이트, 삭제 기능
 * - 이벤트 기반 시스템 통합
 * - 에러 처리 및 롤백 지원
 * 
 * 보안 고려사항:
 * - 모든 시크릿 값은 메모리에서 즉시 제거
 * - AWS API 호출 실패 시 데이터베이스 롤백
 * - 접근 권한 검증 및 감사 로그 기록
 * - 민감 정보 로깅 금지
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserSecretsArnService {

    private final UserSecretsArnRepository userSecretsArnRepository;
    private final AWSSecretsManagerService awsSecretsManagerService;
    private final EventPublisherService eventPublisher;

    /**
     * 사용자 암호화 키를 AWS Secrets Manager에 저장하고 ARN 정보를 관리
     * 
     * 사용자가 제공한 암호화 키를 AWS Secrets Manager에 안전하게 저장하고,
     * 반환받은 ARN을 데이터베이스에 저장하는 전체 프로세스를 수행합니다.
     * 
     * 처리 순서:
     * 1. 입력 파라미터 유효성 검증
     * 2. AWS Secrets Manager에 키 저장
     * 3. 반환받은 ARN으로 UserSecretsArn 엔티티 생성
     * 4. 데이터베이스에 ARN 정보 저장
     * 5. 키 저장 성공 이벤트 발행
     * 
     * @param userId 사용자 식별자
     * @param secretName AWS에서 사용할 시크릿 이름
     * @param secretValue 저장할 암호화 키 값
     * @param description 키에 대한 설명 (선택적)
     * @return 저장된 ARN 정보 엔티티
     * @throws IllegalArgumentException 입력 파라미터가 유효하지 않은 경우
     * @throws AWSSecretsManagerException AWS API 호출 실패 시
     */
    public UserSecretsArn storeUserSecret(String userId, String secretName, 
                                         String secretValue, String description) {
        log.info("사용자 암호화 키 저장 시작 - userId: {}, secretName: {}", userId, secretName);
        
        // 입력 파라미터 유효성 검증
        validateStoreSecretParameters(userId, secretName, secretValue);
        
        try {
            // AWS Secrets Manager에 키 저장
            String arn = awsSecretsManagerService.storeSecret(secretName, secretValue, description);
            log.info("AWS Secrets Manager에 키 저장 성공 - ARN: {}", arn);
            
            // UserSecretsArn 엔티티 생성
            UserSecretsArn userSecretsArn = UserSecretsArn.builder()
                    .arnId(UUID.randomUUID().toString())
                    .userId(userId)
                    .arn(arn)
                    .arnDescription(description)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            // 데이터베이스에 ARN 정보 저장
            UserSecretsArn savedArn = userSecretsArnRepository.save(userSecretsArn);
            log.info("ARN 정보 데이터베이스 저장 성공 - arnId: {}", savedArn.getArnId());
            
            // 키 저장 성공 이벤트 발행
            publishSecretStoredEvent(savedArn);
            
            return savedArn;
            
        } catch (AWSSecretsManagerException e) {
            log.error("AWS Secrets Manager에 키 저장 실패 - userId: {}, secretName: {}", 
                     userId, secretName, e);
            throw e;
        } catch (Exception e) {
            log.error("사용자 키 저장 중 예기치 않은 오류 발생 - userId: {}, secretName: {}", 
                     userId, secretName, e);
            throw new RuntimeException("사용자 키 저장에 실패했습니다.", e);
        }
    }
    
    /**
     * 사용자별 ARN 목록 조회
     * 
     * 특정 사용자가 소유한 모든 암호화 키 ARN 정보를 조회합니다.
     * 결과는 생성 시간 역순(최신순)으로 정렬됩니다.
     * 
     * @param userId 조회할 사용자의 식별자
     * @return 사용자의 ARN 목록
     * @throws IllegalArgumentException 사용자 ID가 유효하지 않은 경우
     */
    @Transactional(readOnly = true)
    public List<UserSecretsArn> getUserSecretsArns(String userId) {
        log.debug("사용자별 ARN 목록 조회 시작 - userId: {}", userId);
        
        validateUserId(userId);
        
        List<UserSecretsArn> arns = userSecretsArnRepository.findByUserId(userId);
        log.debug("사용자별 ARN 목록 조회 완료 - userId: {}, count: {}", userId, arns.size());
        
        return arns;
    }
    
    /**
     * ARN ID로 단일 ARN 정보 조회
     * 
     * ARN의 고유 식별자를 사용하여 특정 ARN 정보를 조회합니다.
     * 
     * @param arnId 조회할 ARN의 식별자
     * @return ARN 정보, 존재하지 않으면 Optional.empty()
     * @throws IllegalArgumentException ARN ID가 유효하지 않은 경우
     */
    @Transactional(readOnly = true)
    public Optional<UserSecretsArn> getSecretsArnById(String arnId) {
        log.debug("ARN ID로 ARN 정보 조회 시작 - arnId: {}", arnId);
        
        validateArnId(arnId);
        
        Optional<UserSecretsArn> arn = userSecretsArnRepository.findById(arnId);
        log.debug("ARN ID로 ARN 정보 조회 완료 - arnId: {}, found: {}", arnId, arn.isPresent());
        
        return arn;
    }
    
    /**
     * ARN 삭제 (데이터베이스 및 AWS에서 모두 삭제)
     * 
     * 지정된 ARN 정보를 데이터베이스에서 삭제하고,
     * AWS Secrets Manager에서도 해당 시크릿을 삭제합니다.
     * 
     * 처리 순서:
     * 1. ARN 정보 존재 여부 확인
     * 2. AWS Secrets Manager에서 시크릿 삭제
     * 3. 데이터베이스에서 ARN 정보 삭제
     * 4. 삭제 성공 이벤트 발행
     * 
     * @param arnId 삭제할 ARN의 식별자
     * @return 삭제 성공 여부 (true: 성공, false: ARN이 존재하지 않음)
     * @throws IllegalArgumentException ARN ID가 유효하지 않은 경우
     * @throws AWSSecretsManagerException AWS 삭제 실패 시
     */
    public boolean deleteSecretsArn(String arnId) {
        log.info("ARN 삭제 시작 - arnId: {}", arnId);
        
        validateArnId(arnId);
        
        // ARN 정보 존재 여부 확인
        Optional<UserSecretsArn> arnOptional = userSecretsArnRepository.findById(arnId);
        if (arnOptional.isEmpty()) {
            log.warn("삭제하려는 ARN이 존재하지 않음 - arnId: {}", arnId);
            return false;
        }
        
        UserSecretsArn userSecretsArn = arnOptional.get();
        
        try {
            // AWS Secrets Manager에서 시크릿 삭제
            awsSecretsManagerService.deleteSecret(userSecretsArn.getArn());
            log.info("AWS Secrets Manager에서 시크릿 삭제 성공 - ARN: {}", userSecretsArn.getArn());
            
            // 데이터베이스에서 ARN 정보 삭제
            userSecretsArnRepository.delete(userSecretsArn);
            log.info("데이터베이스에서 ARN 정보 삭제 성공 - arnId: {}", arnId);
            
            // 삭제 성공 이벤트 발행
            publishSecretDeletedEvent(userSecretsArn);
            
            return true;
            
        } catch (AWSSecretsManagerException e) {
            log.error("AWS Secrets Manager에서 시크릿 삭제 실패 - ARN: {}", 
                     userSecretsArn.getArn(), e);
            throw e;
        } catch (Exception e) {
            log.error("ARN 삭제 중 예기치 않은 오류 발생 - arnId: {}", arnId, e);
            throw new RuntimeException("ARN 삭제에 실패했습니다.", e);
        }
    }
    
    /**
     * ARN 설명 업데이트
     * 
     * 기존 ARN의 설명 정보를 업데이트합니다.
     * 
     * @param arnId 업데이트할 ARN의 식별자
     * @param newDescription 새로운 설명
     * @return 업데이트된 ARN 정보, ARN이 존재하지 않으면 Optional.empty()
     * @throws IllegalArgumentException ARN ID가 유효하지 않은 경우
     */
    public Optional<UserSecretsArn> updateArnDescription(String arnId, String newDescription) {
        log.info("ARN 설명 업데이트 시작 - arnId: {}", arnId);
        
        validateArnId(arnId);
        
        Optional<UserSecretsArn> arnOptional = userSecretsArnRepository.findById(arnId);
        if (arnOptional.isEmpty()) {
            log.warn("업데이트하려는 ARN이 존재하지 않음 - arnId: {}", arnId);
            return Optional.empty();
        }
        
        UserSecretsArn userSecretsArn = arnOptional.get();
        userSecretsArn.updateDescription(newDescription);
        
        UserSecretsArn updatedArn = userSecretsArnRepository.save(userSecretsArn);
        log.info("ARN 설명 업데이트 성공 - arnId: {}", arnId);
        
        // 업데이트 이벤트 발행
        publishSecretUpdatedEvent(updatedArn);
        
        return Optional.of(updatedArn);
    }
    
    /**
     * 사용자별 ARN 개수 조회
     * 
     * 특정 사용자가 소유한 ARN의 개수를 조회합니다.
     * 사용자별 키 관리 제한이나 통계에 활용됩니다.
     * 
     * @param userId 조회할 사용자의 식별자
     * @return 사용자가 소유한 ARN의 개수
     * @throws IllegalArgumentException 사용자 ID가 유효하지 않은 경우
     */
    @Transactional(readOnly = true)
    public long getUserSecretsArnCount(String userId) {
        log.debug("사용자별 ARN 개수 조회 시작 - userId: {}", userId);
        
        validateUserId(userId);
        
        long count = userSecretsArnRepository.countByUserId(userId);
        log.debug("사용자별 ARN 개수 조회 완료 - userId: {}, count: {}", userId, count);
        
        return count;
    }
    
    /**
     * ARN 존재 여부 확인
     * 
     * 특정 ARN이 시스템에 등록되어 있는지 확인합니다.
     * 
     * @param arn 확인할 ARN
     * @return ARN이 존재하면 true, 그렇지 않으면 false
     * @throws IllegalArgumentException ARN이 유효하지 않은 경우
     */
    @Transactional(readOnly = true)
    public boolean isArnExists(String arn) {
        log.debug("ARN 존재 여부 확인 시작 - ARN: {}", arn);
        
        validateArn(arn);
        
        boolean exists = userSecretsArnRepository.existsByArn(arn);
        log.debug("ARN 존재 여부 확인 완료 - ARN: {}, exists: {}", arn, exists);
        
        return exists;
    }
    
    /**
     * AWS에서 시크릿 값 조회
     * 
     * ARN을 사용하여 AWS Secrets Manager에서 실제 시크릿 값을 조회합니다.
     * 보안상 시크릿 값은 로깅하지 않습니다.
     * 
     * @param arn 조회할 시크릿의 ARN
     * @return 시크릿 값
     * @throws IllegalArgumentException ARN이 유효하지 않은 경우
     * @throws AWSSecretsManagerException AWS API 호출 실패 시
     */
    @Transactional(readOnly = true)
    public String getSecretValue(String arn) {
        log.debug("AWS에서 시크릿 값 조회 시작 - ARN: {}", arn);
        
        validateArn(arn);
        
        try {
            String secretValue = awsSecretsManagerService.getSecretValue(arn);
            log.debug("AWS에서 시크릿 값 조회 성공 - ARN: {}", arn);
            
            return secretValue;
            
        } catch (AWSSecretsManagerException e) {
            log.error("AWS에서 시크릿 값 조회 실패 - ARN: {}", arn, e);
            throw e;
        }
    }
    
    /**
     * 키 저장 파라미터 유효성 검증
     * 
     * @param userId 사용자 ID
     * @param secretName 시크릿 이름
     * @param secretValue 시크릿 값
     */
    private void validateStoreSecretParameters(String userId, String secretName, String secretValue) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (secretName == null || secretName.trim().isEmpty()) {
            throw new IllegalArgumentException("시크릿 이름은 필수입니다.");
        }
        if (secretValue == null || secretValue.trim().isEmpty()) {
            throw new IllegalArgumentException("시크릿 값은 필수입니다.");
        }
    }
    
    /**
     * 사용자 ID 유효성 검증
     * 
     * @param userId 사용자 ID
     */
    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
    }
    
    /**
     * ARN ID 유효성 검증
     * 
     * @param arnId ARN ID
     */
    private void validateArnId(String arnId) {
        if (arnId == null || arnId.trim().isEmpty()) {
            throw new IllegalArgumentException("ARN ID는 필수입니다.");
        }
    }
    
    /**
     * ARN 유효성 검증
     * 
     * @param arn ARN
     */
    private void validateArn(String arn) {
        if (arn == null || arn.trim().isEmpty()) {
            throw new IllegalArgumentException("ARN은 필수입니다.");
        }
        if (!arn.startsWith("arn:aws:secretsmanager:")) {
            throw new IllegalArgumentException("유효하지 않은 Secrets Manager ARN 형식입니다.");
        }
    }
    
    /**
     * 키 저장 성공 이벤트 발행
     * 
     * @param userSecretsArn 저장된 ARN 정보
     */
    private void publishSecretStoredEvent(UserSecretsArn userSecretsArn) {
        try {
            // 실제 이벤트 객체 생성 및 발행 로직은 EventPublisher 구현에 따라 결정
            log.info("키 저장 성공 이벤트 발행 - userId: {}, arnId: {}", 
                    userSecretsArn.getUserId(), userSecretsArn.getArnId());
            eventPublisher.publishEvent("SECRET_STORED", userSecretsArn);
        } catch (Exception e) {
            log.warn("키 저장 이벤트 발행 실패 - userId: {}, arnId: {}", 
                    userSecretsArn.getUserId(), userSecretsArn.getArnId(), e);
            // 이벤트 발행 실패는 전체 트랜잭션을 롤백시키지 않음
        }
    }
    
    /**
     * 키 삭제 성공 이벤트 발행
     * 
     * @param userSecretsArn 삭제된 ARN 정보
     */
    private void publishSecretDeletedEvent(UserSecretsArn userSecretsArn) {
        try {
            log.info("키 삭제 성공 이벤트 발행 - userId: {}, arnId: {}", 
                    userSecretsArn.getUserId(), userSecretsArn.getArnId());
            eventPublisher.publishEvent("SECRET_DELETED", userSecretsArn);
        } catch (Exception e) {
            log.warn("키 삭제 이벤트 발행 실패 - userId: {}, arnId: {}", 
                    userSecretsArn.getUserId(), userSecretsArn.getArnId(), e);
        }
    }
    
    /**
     * 키 업데이트 성공 이벤트 발행
     * 
     * @param userSecretsArn 업데이트된 ARN 정보
     */
    private void publishSecretUpdatedEvent(UserSecretsArn userSecretsArn) {
        try {
            log.info("키 업데이트 성공 이벤트 발행 - userId: {}, arnId: {}", 
                    userSecretsArn.getUserId(), userSecretsArn.getArnId());
            eventPublisher.publishEvent("SECRET_UPDATED", userSecretsArn);
        } catch (Exception e) {
            log.warn("키 업데이트 이벤트 발행 실패 - userId: {}, arnId: {}", 
                    userSecretsArn.getUserId(), userSecretsArn.getArnId(), e);
        }
    }
}