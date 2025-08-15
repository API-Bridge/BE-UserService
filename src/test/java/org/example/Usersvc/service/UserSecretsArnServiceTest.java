package org.example.Usersvc.service;

import org.example.Usersvc.domain.UserSecretsArn;
import org.example.Usersvc.repository.UserSecretsArnRepository;
import org.example.Usersvc.event.publisher.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserSecretsArnService 테스트 클래스
 * 
 * TDD 방식으로 UserSecretsArnService의 비즈니스 로직을 테스트합니다.
 * 이 테스트는 UserSecretsArnService 클래스 구현에 앞서 작성되어,
 * AWS Secrets Manager와의 연동 및 ARN 관리 기능의 요구사항을 명확히 정의합니다.
 * 
 * 테스트 대상 기능:
 * - AWS Secrets Manager에 키 저장 및 ARN 반환
 * - ARN 정보의 데이터베이스 저장
 * - 사용자별 ARN 조회 및 관리
 * - ARN 삭제 및 AWS 리소스 정리
 * - 에러 처리 및 예외 상황 대응
 * 
 * Mock 객체 사용:
 * - AWSSecretsManagerClient: AWS API 호출 시뮬레이션
 * - UserSecretsArnRepository: 데이터베이스 연산 시뮬레이션
 * - EventPublisher: 이벤트 발행 시뮬레이션
 */
@ExtendWith(MockitoExtension.class)
class UserSecretsArnServiceTest {

    @Mock
    private UserSecretsArnRepository userSecretsArnRepository;
    
    @Mock
    private AWSSecretsManagerService awsSecretsManagerService;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @InjectMocks
    private UserSecretsArnService userSecretsArnService;
    
    private String testUserId;
    private String testSecretValue;
    private String testArn;
    private UserSecretsArn testUserSecretsArn;
    
    /**
     * 각 테스트 메서드 실행 전 테스트 데이터 초기화
     * 
     * 테스트에 필요한 공통 데이터를 설정하여
     * 일관된 테스트 환경을 제공합니다.
     */
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();
        testSecretValue = "test-secret-key-value";
        testArn = "arn:aws:secretsmanager:us-east-1:123456789012:secret:user-key-AbCdEf";
        
        testUserSecretsArn = UserSecretsArn.builder()
                .arnId(UUID.randomUUID().toString())
                .userId(testUserId)
                .arn(testArn)
                .arnDescription("Test encryption key")
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 사용자 키 저장 성공 시나리오 테스트
     * 
     * 사용자가 제공한 암호화 키를 AWS Secrets Manager에 저장하고,
     * 반환받은 ARN을 데이터베이스에 저장하는 전체 프로세스를 검증합니다.
     * 
     * 테스트 시나리오:
     * 1. AWS Secrets Manager에 키 저장 요청
     * 2. AWS에서 ARN 반환
     * 3. ARN 정보를 데이터베이스에 저장
     * 4. 성공 이벤트 발행
     */
    @Test
    @DisplayName("사용자 키를 AWS Secrets Manager에 저장하고 ARN을 데이터베이스에 저장할 수 있다")
    void storeUserSecret_Success() {
        // given: AWS와 데이터베이스 연산이 성공하도록 Mock 설정
        when(awsSecretsManagerService.storeSecret(anyString(), anyString(), anyString()))
                .thenReturn(testArn);
        when(userSecretsArnRepository.save(any(UserSecretsArn.class)))
                .thenReturn(testUserSecretsArn);
        
        // when: 사용자 키 저장 요청
        String secretName = "user-" + testUserId + "-key";
        String description = "User's personal encryption key";
        UserSecretsArn result = userSecretsArnService.storeUserSecret(
                testUserId, secretName, testSecretValue, description);
        
        // then: 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getArn()).isEqualTo(testArn);
        assertThat(result.getArnDescription()).isEqualTo(description);
        
        // AWS 서비스 호출 검증
        verify(awsSecretsManagerService).storeSecret(eq(secretName), eq(testSecretValue), eq(description));
        
        // 데이터베이스 저장 검증
        verify(userSecretsArnRepository).save(any(UserSecretsArn.class));
        
        // 이벤트 발행 검증
        verify(eventPublisher).publishEvent(anyString(), any());
    }
    
    /**
     * AWS Secrets Manager 저장 실패 시나리오 테스트
     * 
     * AWS API 호출이 실패했을 때 적절한 예외가 발생하고,
     * 데이터베이스에는 저장되지 않는지 검증합니다.
     */
    @Test
    @DisplayName("AWS Secrets Manager 저장 실패 시 예외가 발생한다")
    void storeUserSecret_AWSFailure() {
        // given: AWS 저장이 실패하도록 Mock 설정
        when(awsSecretsManagerService.storeSecret(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("AWS API Error"));
        
        // when & then: 예외 발생 검증
        assertThatThrownBy(() -> {
            userSecretsArnService.storeUserSecret(
                    testUserId, "test-key", testSecretValue, "description");
        }).isInstanceOf(RuntimeException.class)
          .hasMessageContaining("AWS API Error");
        
        // 데이터베이스 저장이 호출되지 않았는지 검증
        verify(userSecretsArnRepository, never()).save(any(UserSecretsArn.class));
        
        // 이벤트가 발행되지 않았는지 검증
        verify(eventPublisher, never()).publishEvent(anyString(), any());
    }
    
    /**
     * 사용자별 ARN 목록 조회 테스트
     * 
     * 특정 사용자가 소유한 모든 ARN 정보를 조회하는 기능을 검증합니다.
     */
    @Test
    @DisplayName("사용자별 ARN 목록을 조회할 수 있다")
    void getUserSecretsArns_Success() {
        // given: 사용자가 여러 ARN을 소유하는 상황
        UserSecretsArn secondArn = UserSecretsArn.builder()
                .arnId(UUID.randomUUID().toString())
                .userId(testUserId)
                .arn("arn:aws:secretsmanager:us-east-1:123456789012:secret:user-key-2-XyZwVu")
                .arnDescription("Backup encryption key")
                .createdAt(LocalDateTime.now())
                .build();
        
        List<UserSecretsArn> mockArns = Arrays.asList(testUserSecretsArn, secondArn);
        when(userSecretsArnRepository.findByUserId(testUserId)).thenReturn(mockArns);
        
        // when: 사용자별 ARN 목록 조회
        List<UserSecretsArn> result = userSecretsArnService.getUserSecretsArns(testUserId);
        
        // then: 결과 검증
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(testUserSecretsArn, secondArn);
        
        // Repository 호출 검증
        verify(userSecretsArnRepository).findByUserId(testUserId);
    }
    
    /**
     * ARN ID로 단일 ARN 조회 테스트
     * 
     * ARN의 고유 식별자를 사용하여 특정 ARN 정보를 조회하는 기능을 검증합니다.
     */
    @Test
    @DisplayName("ARN ID로 단일 ARN 정보를 조회할 수 있다")
    void getSecretsArnById_Success() {
        // given: 특정 ARN이 존재하도록 Mock 설정
        when(userSecretsArnRepository.findById(testUserSecretsArn.getArnId()))
                .thenReturn(Optional.of(testUserSecretsArn));
        
        // when: ARN ID로 조회
        Optional<UserSecretsArn> result = userSecretsArnService.getSecretsArnById(testUserSecretsArn.getArnId());
        
        // then: 결과 검증
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUserSecretsArn);
        
        // Repository 호출 검증
        verify(userSecretsArnRepository).findById(testUserSecretsArn.getArnId());
    }
    
    /**
     * 존재하지 않는 ARN ID 조회 테스트
     * 
     * 존재하지 않는 ARN ID로 조회할 때 빈 결과를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("존재하지 않는 ARN ID로 조회 시 빈 결과를 반환한다")
    void getSecretsArnById_NotFound() {
        // given: ARN이 존재하지 않도록 Mock 설정
        String nonExistentId = "non-existent-id";
        when(userSecretsArnRepository.findById(nonExistentId))
                .thenReturn(Optional.empty());
        
        // when: 존재하지 않는 ID로 조회
        Optional<UserSecretsArn> result = userSecretsArnService.getSecretsArnById(nonExistentId);
        
        // then: 빈 결과 검증
        assertThat(result).isNotPresent();
        
        // Repository 호출 검증
        verify(userSecretsArnRepository).findById(nonExistentId);
    }
    
    /**
     * ARN 삭제 성공 시나리오 테스트
     * 
     * ARN 정보를 데이터베이스에서 삭제하고,
     * AWS Secrets Manager에서도 해당 시크릿을 삭제하는 기능을 검증합니다.
     */
    @Test
    @DisplayName("ARN을 데이터베이스와 AWS에서 성공적으로 삭제할 수 있다")
    void deleteSecretsArn_Success() {
        // given: ARN이 존재하고 삭제가 성공하도록 Mock 설정
        when(userSecretsArnRepository.findById(testUserSecretsArn.getArnId()))
                .thenReturn(Optional.of(testUserSecretsArn));
        doNothing().when(awsSecretsManagerService).deleteSecret(testArn);
        doNothing().when(userSecretsArnRepository).delete(testUserSecretsArn);
        
        // when: ARN 삭제 요청
        boolean result = userSecretsArnService.deleteSecretsArn(testUserSecretsArn.getArnId());
        
        // then: 결과 검증
        assertThat(result).isTrue();
        
        // AWS 삭제 호출 검증
        verify(awsSecretsManagerService).deleteSecret(testArn);
        
        // 데이터베이스 삭제 호출 검증
        verify(userSecretsArnRepository).delete(testUserSecretsArn);
        
        // 삭제 이벤트 발행 검증
        verify(eventPublisher).publishEvent(anyString(), any());
    }
    
    /**
     * 존재하지 않는 ARN 삭제 시나리오 테스트
     * 
     * 존재하지 않는 ARN ID로 삭제를 요청했을 때
     * 적절히 처리되는지 검증합니다.
     */
    @Test
    @DisplayName("존재하지 않는 ARN 삭제 시 false를 반환한다")
    void deleteSecretsArn_NotFound() {
        // given: ARN이 존재하지 않도록 Mock 설정
        String nonExistentId = "non-existent-id";
        when(userSecretsArnRepository.findById(nonExistentId))
                .thenReturn(Optional.empty());
        
        // when: 존재하지 않는 ARN 삭제 요청
        boolean result = userSecretsArnService.deleteSecretsArn(nonExistentId);
        
        // then: 결과 검증
        assertThat(result).isFalse();
        
        // AWS 삭제가 호출되지 않았는지 검증
        verify(awsSecretsManagerService, never()).deleteSecret(anyString());
        
        // 데이터베이스 삭제가 호출되지 않았는지 검증
        verify(userSecretsArnRepository, never()).delete(any(UserSecretsArn.class));
        
        // 이벤트가 발행되지 않았는지 검증
        verify(eventPublisher, never()).publishEvent(anyString(), any());
    }
    
    /**
     * ARN 설명 업데이트 테스트
     * 
     * 기존 ARN의 설명 정보를 업데이트하는 기능을 검증합니다.
     */
    @Test
    @DisplayName("ARN 설명을 업데이트할 수 있다")
    void updateArnDescription_Success() {
        // given: ARN이 존재하고 업데이트가 성공하도록 Mock 설정
        String newDescription = "Updated encryption key description";
        UserSecretsArn updatedArn = UserSecretsArn.builder()
                .arnId(testUserSecretsArn.getArnId())
                .userId(testUserSecretsArn.getUserId())
                .arn(testUserSecretsArn.getArn())
                .arnDescription(newDescription)
                .createdAt(testUserSecretsArn.getCreatedAt())
                .build();
        
        when(userSecretsArnRepository.findById(testUserSecretsArn.getArnId()))
                .thenReturn(Optional.of(testUserSecretsArn));
        when(userSecretsArnRepository.save(any(UserSecretsArn.class)))
                .thenReturn(updatedArn);
        
        // when: ARN 설명 업데이트
        Optional<UserSecretsArn> result = userSecretsArnService.updateArnDescription(
                testUserSecretsArn.getArnId(), newDescription);
        
        // then: 결과 검증
        assertThat(result).isPresent();
        assertThat(result.get().getArnDescription()).isEqualTo(newDescription);
        
        // Repository 호출 검증
        verify(userSecretsArnRepository).findById(testUserSecretsArn.getArnId());
        verify(userSecretsArnRepository).save(any(UserSecretsArn.class));
        
        // 업데이트 이벤트 발행 검증
        verify(eventPublisher).publishEvent(anyString(), any());
    }
    
    /**
     * 사용자별 ARN 개수 조회 테스트
     * 
     * 특정 사용자가 소유한 ARN의 개수를 조회하는 기능을 검증합니다.
     */
    @Test
    @DisplayName("사용자별 ARN 개수를 조회할 수 있다")
    void getUserSecretsArnCount_Success() {
        // given: 사용자가 3개의 ARN을 소유하도록 Mock 설정
        long expectedCount = 3L;
        when(userSecretsArnRepository.countByUserId(testUserId)).thenReturn(expectedCount);
        
        // when: 사용자별 ARN 개수 조회
        long result = userSecretsArnService.getUserSecretsArnCount(testUserId);
        
        // then: 결과 검증
        assertThat(result).isEqualTo(expectedCount);
        
        // Repository 호출 검증
        verify(userSecretsArnRepository).countByUserId(testUserId);
    }
    
    /**
     * ARN 존재 여부 확인 테스트
     * 
     * 특정 ARN이 시스템에 등록되어 있는지 확인하는 기능을 검증합니다.
     */
    @Test
    @DisplayName("ARN 존재 여부를 확인할 수 있다")
    void isArnExists_Success() {
        // given: ARN이 존재하도록 Mock 설정
        when(userSecretsArnRepository.existsByArn(testArn)).thenReturn(true);
        
        // when: ARN 존재 여부 확인
        boolean result = userSecretsArnService.isArnExists(testArn);
        
        // then: 결과 검증
        assertThat(result).isTrue();
        
        // Repository 호출 검증
        verify(userSecretsArnRepository).existsByArn(testArn);
    }
    
    /**
     * AWS에서 시크릿 값 조회 테스트
     * 
     * ARN을 사용하여 AWS Secrets Manager에서 실제 시크릿 값을 조회하는 기능을 검증합니다.
     */
    @Test
    @DisplayName("ARN을 사용하여 AWS에서 시크릿 값을 조회할 수 있다")
    void getSecretValue_Success() {
        // given: AWS에서 시크릿 값을 반환하도록 Mock 설정
        String expectedSecretValue = "retrieved-secret-value";
        when(awsSecretsManagerService.getSecretValue(testArn)).thenReturn(expectedSecretValue);
        
        // when: 시크릿 값 조회
        String result = userSecretsArnService.getSecretValue(testArn);
        
        // then: 결과 검증
        assertThat(result).isEqualTo(expectedSecretValue);
        
        // AWS 서비스 호출 검증
        verify(awsSecretsManagerService).getSecretValue(testArn);
    }
    
    /**
     * 잘못된 입력 파라미터 검증 테스트
     * 
     * null이나 빈 문자열 등 잘못된 입력에 대한 검증을 확인합니다.
     */
    @Test
    @DisplayName("잘못된 입력 파라미터에 대해 예외가 발생한다")
    void validateInputParameters() {
        // when & then: null 사용자 ID로 키 저장 시도
        assertThatThrownBy(() -> {
            userSecretsArnService.storeUserSecret(null, "key-name", "secret-value", "description");
        }).isInstanceOf(IllegalArgumentException.class);
        
        // when & then: 빈 키 이름으로 저장 시도
        assertThatThrownBy(() -> {
            userSecretsArnService.storeUserSecret(testUserId, "", "secret-value", "description");
        }).isInstanceOf(IllegalArgumentException.class);
        
        // when & then: null 시크릿 값으로 저장 시도
        assertThatThrownBy(() -> {
            userSecretsArnService.storeUserSecret(testUserId, "key-name", null, "description");
        }).isInstanceOf(IllegalArgumentException.class);
    }
}