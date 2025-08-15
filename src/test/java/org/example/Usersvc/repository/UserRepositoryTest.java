package org.example.Usersvc.repository;

import org.example.Usersvc.domain.User;
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
 * User Repository 테스트 클래스
 * 
 * TDD 방식으로 User 엔티티의 데이터 액세스 계층을 테스트합니다.
 * 이 테스트는 User 엔티티와 UserRepository 구현에 앞서 작성되어,
 * 요구사항을 명확히 정의하고 구현 가이드를 제공합니다.
 * 
 * 테스트 대상 기능:
 * - 사용자 저장 (Auth0 ID, 이메일 포함)
 * - 사용자 조회 (ID, Auth0 ID, 이메일 기준)
 * - 사용자 삭제
 * - 데이터 무결성 검증 (유니크 제약조건)
 */
class UserRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    
    /**
     * 각 테스트 메서드 실행 전 테스트 데이터 초기화
     * 
     * 테스트용 User 객체를 생성하여 일관된 테스트 환경을 제공합니다.
     * UUID 기반의 고유 식별자와 Auth0 연동을 위한 외부 ID를 포함합니다.
     */
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(UUID.randomUUID().toString())
                .auth0Id("auth0|test123456789")
                .userEmail("test@example.com")
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 사용자 저장 기능 테스트
     * 
     * 새로운 사용자 정보를 데이터베이스에 저장하고,
     * 저장된 데이터의 무결성을 검증합니다.
     * 
     * 검증 항목:
     * - 저장 후 ID가 올바르게 할당되는지
     * - 모든 필드가 정확히 저장되는지
     * - 생성 시간이 자동으로 설정되는지
     */
    @Test
    @DisplayName("사용자 정보를 성공적으로 저장할 수 있다")
    void saveUser_Success() {
        // when: 사용자 정보를 저장
        User savedUser = userRepository.save(testUser);
        
        // then: 저장된 사용자 정보 검증
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getUserId()).isEqualTo(testUser.getUserId());
        assertThat(savedUser.getAuth0Id()).isEqualTo(testUser.getAuth0Id());
        assertThat(savedUser.getUserEmail()).isEqualTo(testUser.getUserEmail());
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }
    
    /**
     * 사용자 ID로 조회 기능 테스트
     * 
     * 사용자 ID를 사용하여 저장된 사용자 정보를 조회하고,
     * 조회된 데이터의 정확성을 검증합니다.
     */
    @Test
    @DisplayName("사용자 ID로 사용자를 조회할 수 있다")
    void findByUserId_Success() {
        // given: 테스트 사용자 저장
        entityManager.persistAndFlush(testUser);
        
        // when: 사용자 ID로 조회
        Optional<User> foundUser = userRepository.findById(testUser.getUserId());
        
        // then: 조회 결과 검증
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUserId()).isEqualTo(testUser.getUserId());
        assertThat(foundUser.get().getAuth0Id()).isEqualTo(testUser.getAuth0Id());
        assertThat(foundUser.get().getUserEmail()).isEqualTo(testUser.getUserEmail());
    }
    
    /**
     * Auth0 ID로 조회 기능 테스트
     * 
     * Auth0에서 제공하는 외부 ID를 사용하여 사용자를 조회합니다.
     * 이는 Auth0 연동 시 필수적인 기능입니다.
     */
    @Test
    @DisplayName("Auth0 ID로 사용자를 조회할 수 있다")
    void findByAuth0Id_Success() {
        // given: 테스트 사용자 저장
        entityManager.persistAndFlush(testUser);
        
        // when: Auth0 ID로 조회
        Optional<User> foundUser = userRepository.findByAuth0Id(testUser.getAuth0Id());
        
        // then: 조회 결과 검증
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getAuth0Id()).isEqualTo(testUser.getAuth0Id());
        assertThat(foundUser.get().getUserEmail()).isEqualTo(testUser.getUserEmail());
    }
    
    /**
     * 이메일로 조회 기능 테스트
     * 
     * 사용자 이메일을 사용하여 사용자를 조회합니다.
     * 이메일은 사용자 식별의 중요한 수단입니다.
     */
    @Test
    @DisplayName("이메일로 사용자를 조회할 수 있다")
    void findByUserEmail_Success() {
        // given: 테스트 사용자 저장
        entityManager.persistAndFlush(testUser);
        
        // when: 이메일로 조회
        Optional<User> foundUser = userRepository.findByUserEmail(testUser.getUserEmail());
        
        // then: 조회 결과 검증
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUserEmail()).isEqualTo(testUser.getUserEmail());
        assertThat(foundUser.get().getAuth0Id()).isEqualTo(testUser.getAuth0Id());
    }
    
    /**
     * 사용자 삭제 기능 테스트
     * 
     * 저장된 사용자를 삭제하고, 삭제가 정상적으로 수행되었는지 검증합니다.
     */
    @Test
    @DisplayName("사용자를 성공적으로 삭제할 수 있다")
    void deleteUser_Success() {
        // given: 테스트 사용자 저장
        entityManager.persistAndFlush(testUser);
        
        // when: 사용자 삭제
        userRepository.delete(testUser);
        entityManager.flush();
        
        // then: 삭제 검증
        Optional<User> deletedUser = userRepository.findById(testUser.getUserId());
        assertThat(deletedUser).isNotPresent();
    }
    
    /**
     * Auth0 ID 유니크 제약조건 테스트
     * 
     * 동일한 Auth0 ID를 가진 사용자가 중복 저장되지 않는지 검증합니다.
     * 데이터베이스 레벨의 무결성 제약조건을 테스트합니다.
     */
    @Test
    @DisplayName("동일한 Auth0 ID로 중복 사용자를 저장할 수 없다")
    void saveUserWithDuplicateAuth0Id_ShouldFail() {
        // given: 첫 번째 사용자 저장
        entityManager.persistAndFlush(testUser);
        
        // when & then: 동일한 Auth0 ID로 두 번째 사용자 저장 시도
        User duplicateUser = User.builder()
                .userId(UUID.randomUUID().toString())
                .auth0Id(testUser.getAuth0Id())  // 동일한 Auth0 ID
                .userEmail("different@example.com")
                .createdAt(LocalDateTime.now())
                .build();
        
        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(duplicateUser);
        }).isInstanceOf(Exception.class);
    }
    
    /**
     * 이메일 유니크 제약조건 테스트
     * 
     * 동일한 이메일을 가진 사용자가 중복 저장되지 않는지 검증합니다.
     * 이메일의 유일성을 보장하는 데이터베이스 제약조건을 테스트합니다.
     */
    @Test
    @DisplayName("동일한 이메일로 중복 사용자를 저장할 수 없다")
    void saveUserWithDuplicateEmail_ShouldFail() {
        // given: 첫 번째 사용자 저장
        entityManager.persistAndFlush(testUser);
        
        // when & then: 동일한 이메일로 두 번째 사용자 저장 시도
        User duplicateUser = User.builder()
                .userId(UUID.randomUUID().toString())
                .auth0Id("auth0|different123456789")
                .userEmail(testUser.getUserEmail())  // 동일한 이메일
                .createdAt(LocalDateTime.now())
                .build();
        
        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(duplicateUser);
        }).isInstanceOf(Exception.class);
    }
    
    /**
     * 존재하지 않는 사용자 조회 테스트
     * 
     * 존재하지 않는 사용자 ID로 조회 시 적절한 결과를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회 시 빈 결과를 반환한다")
    void findByNonExistentUserId_ShouldReturnEmpty() {
        // when: 존재하지 않는 ID로 조회
        Optional<User> result = userRepository.findById("non-existent-id");
        
        // then: 빈 결과 검증
        assertThat(result).isNotPresent();
    }
    
    /**
     * 모든 사용자 조회 기능 테스트
     * 
     * 저장된 모든 사용자를 조회하고, 결과의 정확성을 검증합니다.
     */
    @Test
    @DisplayName("저장된 모든 사용자를 조회할 수 있다")
    void findAllUsers_Success() {
        // given: 여러 사용자 저장
        User secondUser = User.builder()
                .userId(UUID.randomUUID().toString())
                .auth0Id("auth0|test987654321")
                .userEmail("test2@example.com")
                .createdAt(LocalDateTime.now())
                .build();
        
        entityManager.persist(testUser);
        entityManager.persist(secondUser);
        entityManager.flush();
        
        // when: 모든 사용자 조회
        List<User> allUsers = userRepository.findAll();
        
        // then: 조회 결과 검증
        assertThat(allUsers).hasSize(2);
        assertThat(allUsers).extracting("userEmail")
                .contains(testUser.getUserEmail(), secondUser.getUserEmail());
    }
}