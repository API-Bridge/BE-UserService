package org.example.Usersvc.repository;

import org.example.Usersvc.domain.UserSecretsArn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * UserSecretsArn Repository 테스트 클래스
 * 
 * TDD 방식으로 UserSecretsArn 엔티티의 데이터 액세스 계층을 테스트합니다.
 * 이 테스트는 UserSecretsArn 엔티티와 UserSecretsArnRepository 구현에 앞서 작성되어,
 * AWS Secrets Manager ARN 관리 기능의 요구사항을 명확히 정의합니다.
 * 
 * 테스트 대상 기능:
 * - ARN 정보 저장 및 조회
 * - 사용자별 ARN 목록 조회
 * - ARN 설명 정보 관리
 * - 생성 시간 자동 관리
 * - 데이터 무결성 검증
 * 
 * BYOK(Bring Your Own Key) 시나리오:
 * - 사용자가 자체 암호화 키를 AWS Secrets Manager에 저장
 * - 반환된 ARN을 시스템에서 관리
 * - 사용자별 다중 키 관리 지원
 */
class UserSecretsArnRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserSecretsArnRepository userSecretsArnRepository;
    
    private UserSecretsArn testUserSecretsArn;
    private String testUserId;
    
    /**
     * 각 테스트 메서드 실행 전 테스트 데이터 초기화
     * 
     * 테스트용 UserSecretsArn 객체를 생성하여 일관된 테스트 환경을 제공합니다.
     * AWS Secrets Manager에서 반환받은 ARN을 시뮬레이션합니다.
     */
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();
        
        testUserSecretsArn = UserSecretsArn.builder()
                .arnId(UUID.randomUUID().toString())
                .userId(testUserId)
                .arn("arn:aws:secretsmanager:us-east-1:123456789012:secret:user-key-AbCdEf")
                .arnDescription("User's personal encryption key")
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * ARN 정보 저장 기능 테스트
     * 
     * AWS Secrets Manager에 저장된 사용자 키의 ARN 정보를
     * 데이터베이스에 성공적으로 저장하는지 검증합니다.
     * 
     * 검증 항목:
     * - ARN 정보가 정확히 저장되는지
     * - 사용자 ID 연결이 올바른지
     * - 생성 시간이 자동으로 설정되는지
     * - 설명 정보가 저장되는지
     */
    @Test
    @DisplayName("사용자 ARN 정보를 성공적으로 저장할 수 있다")
    void saveUserSecretsArn_Success() {
        // when: ARN 정보를 저장
        UserSecretsArn savedArn = userSecretsArnRepository.save(testUserSecretsArn);
        
        // then: 저장된 ARN 정보 검증
        assertThat(savedArn).isNotNull();
        assertThat(savedArn.getArnId()).isEqualTo(testUserSecretsArn.getArnId());
        assertThat(savedArn.getUserId()).isEqualTo(testUserSecretsArn.getUserId());
        assertThat(savedArn.getArn()).isEqualTo(testUserSecretsArn.getArn());
        assertThat(savedArn.getArnDescription()).isEqualTo(testUserSecretsArn.getArnDescription());
        assertThat(savedArn.getCreatedAt()).isNotNull();
    }
    
    /**
     * ARN ID로 조회 기능 테스트
     * 
     * ARN의 고유 식별자를 사용하여 저장된 ARN 정보를 조회하고,
     * 조회된 데이터의 정확성을 검증합니다.
     */
    @Test
    @DisplayName("ARN ID로 ARN 정보를 조회할 수 있다")
    void findByArnId_Success() {
        // given: 테스트 ARN 정보 저장
        entityManager.persistAndFlush(testUserSecretsArn);
        
        // when: ARN ID로 조회
        Optional<UserSecretsArn> foundArn = userSecretsArnRepository.findById(testUserSecretsArn.getArnId());
        
        // then: 조회 결과 검증
        assertThat(foundArn).isPresent();
        assertThat(foundArn.get().getArnId()).isEqualTo(testUserSecretsArn.getArnId());
        assertThat(foundArn.get().getUserId()).isEqualTo(testUserSecretsArn.getUserId());
        assertThat(foundArn.get().getArn()).isEqualTo(testUserSecretsArn.getArn());
    }
    
    /**
     * 사용자 ID로 ARN 목록 조회 기능 테스트
     * 
     * 특정 사용자가 소유한 모든 ARN 정보를 조회하는 기능을 검증합니다.
     * 사용자는 여러 개의 암호화 키를 가질 수 있으므로 목록 조회가 필요합니다.
     */
    @Test
    @DisplayName("사용자 ID로 해당 사용자의 모든 ARN을 조회할 수 있다")
    void findByUserId_Success() {
        // given: 동일 사용자의 여러 ARN 정보 저장
        UserSecretsArn secondArn = UserSecretsArn.builder()
                .arnId(UUID.randomUUID().toString())
                .userId(testUserId)
                .arn("arn:aws:secretsmanager:us-east-1:123456789012:secret:user-key-2-XyZwVu")
                .arnDescription("User's backup encryption key")
                .createdAt(LocalDateTime.now())
                .build();
        
        entityManager.persist(testUserSecretsArn);
        entityManager.persist(secondArn);
        entityManager.flush();
        
        // when: 사용자 ID로 ARN 목록 조회
        List<UserSecretsArn> userArns = userSecretsArnRepository.findByUserId(testUserId);
        
        // then: 조회 결과 검증
        assertThat(userArns).hasSize(2);
        assertThat(userArns).extracting("userId").containsOnly(testUserId);
        assertThat(userArns).extracting("arn")
                .contains(testUserSecretsArn.getArn(), secondArn.getArn());
    }
    
    /**
     * ARN 문자열로 조회 기능 테스트
     * 
     * AWS Secrets Manager의 실제 ARN 문자열을 사용하여
     * 해당하는 ARN 정보를 조회하는 기능을 검증합니다.
     */
    @Test
    @DisplayName("ARN 문자열로 ARN 정보를 조회할 수 있다")
    void findByArn_Success() {
        // given: 테스트 ARN 정보 저장
        entityManager.persistAndFlush(testUserSecretsArn);
        
        // when: ARN 문자열로 조회
        Optional<UserSecretsArn> foundArn = userSecretsArnRepository.findByArn(testUserSecretsArn.getArn());
        
        // then: 조회 결과 검증
        assertThat(foundArn).isPresent();
        assertThat(foundArn.get().getArn()).isEqualTo(testUserSecretsArn.getArn());
        assertThat(foundArn.get().getUserId()).isEqualTo(testUserSecretsArn.getUserId());
    }
    
    /**
     * ARN 삭제 기능 테스트
     * 
     * 저장된 ARN 정보를 삭제하고, 삭제가 정상적으로 수행되었는지 검증합니다.
     * 사용자가 키를 교체하거나 삭제할 때 필요한 기능입니다.
     */
    @Test
    @DisplayName("ARN 정보를 성공적으로 삭제할 수 있다")
    void deleteUserSecretsArn_Success() {
        // given: 테스트 ARN 정보 저장
        entityManager.persistAndFlush(testUserSecretsArn);
        
        // when: ARN 정보 삭제
        userSecretsArnRepository.delete(testUserSecretsArn);
        entityManager.flush();
        
        // then: 삭제 검증
        Optional<UserSecretsArn> deletedArn = userSecretsArnRepository.findById(testUserSecretsArn.getArnId());
        assertThat(deletedArn).isNotPresent();
    }
    
    /**
     * 사용자별 ARN 개수 조회 기능 테스트
     * 
     * 특정 사용자가 소유한 ARN의 개수를 조회하는 기능을 검증합니다.
     * 사용자별 키 관리 제한이나 통계에 활용됩니다.
     */
    @Test
    @DisplayName("사용자별 ARN 개수를 조회할 수 있다")
    void countByUserId_Success() {
        // given: 동일 사용자의 여러 ARN 정보 저장
        UserSecretsArn secondArn = UserSecretsArn.builder()
                .arnId(UUID.randomUUID().toString())
                .userId(testUserId)
                .arn("arn:aws:secretsmanager:us-east-1:123456789012:secret:user-key-2-XyZwVu")
                .arnDescription("User's backup encryption key")
                .createdAt(LocalDateTime.now())
                .build();
        
        entityManager.persist(testUserSecretsArn);
        entityManager.persist(secondArn);
        entityManager.flush();
        
        // when: 사용자별 ARN 개수 조회
        long arnCount = userSecretsArnRepository.countByUserId(testUserId);
        
        // then: 개수 검증
        assertThat(arnCount).isEqualTo(2);
    }
    
    /**
     * ARN 존재 여부 확인 기능 테스트
     * 
     * 특정 ARN이 이미 시스템에 등록되어 있는지 확인하는 기능을 검증합니다.
     * 중복 ARN 등록 방지에 활용됩니다.
     */
    @Test
    @DisplayName("ARN 존재 여부를 확인할 수 있다")
    void existsByArn_Success() {
        // given: 테스트 ARN 정보 저장
        entityManager.persistAndFlush(testUserSecretsArn);
        
        // when: ARN 존재 여부 확인
        boolean exists = userSecretsArnRepository.existsByArn(testUserSecretsArn.getArn());
        boolean notExists = userSecretsArnRepository.existsByArn("arn:aws:secretsmanager:us-east-1:123456789012:secret:non-existent");
        
        // then: 존재 여부 검증
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
    
    /**
     * 생성 날짜 범위로 ARN 조회 기능 테스트
     * 
     * 특정 기간 동안 생성된 ARN 정보들을 조회하는 기능을 검증합니다.
     * 통계 분석이나 감사 목적으로 활용됩니다.
     */
    @Test
    @DisplayName("생성 날짜 범위로 ARN 목록을 조회할 수 있다")
    void findByCreatedAtBetween_Success() {
        // given: 서로 다른 시간에 생성된 ARN 정보들 저장
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        
        UserSecretsArn oldArn = UserSecretsArn.builder()
                .arnId(UUID.randomUUID().toString())
                .userId(UUID.randomUUID().toString())
                .arn("arn:aws:secretsmanager:us-east-1:123456789012:secret:old-key-AbCdEf")
                .arnDescription("Old encryption key")
                .createdAt(yesterday)
                .build();
        
        entityManager.persist(testUserSecretsArn);
        entityManager.persist(oldArn);
        entityManager.flush();
        
        // when: 오늘 생성된 ARN만 조회
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        
        List<UserSecretsArn> todayArns = userSecretsArnRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        
        // then: 오늘 생성된 ARN만 조회되는지 검증
        assertThat(todayArns).hasSize(1);
        assertThat(todayArns.get(0).getArnId()).isEqualTo(testUserSecretsArn.getArnId());
    }
    
    /**
     * 존재하지 않는 사용자 ID로 조회 테스트
     * 
     * 존재하지 않는 사용자 ID로 ARN을 조회할 때 
     * 적절한 빈 결과를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회 시 빈 목록을 반환한다")
    void findByNonExistentUserId_ShouldReturnEmptyList() {
        // given: 테스트 ARN 정보 저장
        entityManager.persistAndFlush(testUserSecretsArn);
        
        // when: 존재하지 않는 사용자 ID로 조회
        List<UserSecretsArn> result = userSecretsArnRepository.findByUserId("non-existent-user-id");
        
        // then: 빈 목록 반환 검증
        assertThat(result).isEmpty();
    }
    
    /**
     * ARN 설명 업데이트 기능 테스트
     * 
     * 저장된 ARN의 설명 정보를 업데이트하는 기능을 검증합니다.
     * 사용자가 키의 용도나 설명을 변경할 때 필요한 기능입니다.
     */
    @Test
    @DisplayName("ARN 설명을 업데이트할 수 있다")
    void updateArnDescription_Success() {
        // given: 테스트 ARN 정보 저장
        entityManager.persistAndFlush(testUserSecretsArn);
        
        // when: ARN 설명 업데이트
        String newDescription = "Updated encryption key description";
        testUserSecretsArn.updateDescription(newDescription);
        UserSecretsArn updatedArn = userSecretsArnRepository.save(testUserSecretsArn);
        
        // then: 업데이트된 설명 검증
        assertThat(updatedArn.getArnDescription()).isEqualTo(newDescription);
        
        // 데이터베이스에서 다시 조회하여 검증
        Optional<UserSecretsArn> reloadedArn = userSecretsArnRepository.findById(testUserSecretsArn.getArnId());
        assertThat(reloadedArn).isPresent();
        assertThat(reloadedArn.get().getArnDescription()).isEqualTo(newDescription);
    }
}