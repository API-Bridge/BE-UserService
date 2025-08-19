package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.event.model.UserCreatedEvent;
import org.example.Usersvc.event.model.UserDeletedEvent;
import org.example.Usersvc.event.publisher.EventPublisherService;
import org.example.Usersvc.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.Usersvc.util.UserIdGenerator;

/**
 * 사용자 관리 서비스
 * 
 * 사용자의 생성, 조회, 수정, 삭제 등 핵심 비즈니스 로직을 담당하는 서비스입니다.
 * Auth0와 연동하여 외부 인증 시스템의 사용자 정보를 
 * 내부 시스템에서 관리하는 기능을 제공합니다.
 * 
 * 주요 기능:
 * - 사용자 생성 및 Auth0 연동
 * - 사용자 정보 조회 (ID, Auth0 ID, 이메일 기준)
 * - 사용자 정보 업데이트
 * - 사용자 삭제 및 관련 리소스 정리
 * - 이벤트 기반 시스템 통합
 * 
 * 이벤트 발행:
 * - USER_CREATED: 새 사용자 생성 시
 * - USER_DELETED: 사용자 삭제 시
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final EventPublisherService eventPublisher;

    /**
     * 새로운 사용자 생성
     * 
     * Auth0 인증 후 사용자 정보를 시스템에 등록합니다.
     * 사용자 생성 성공 시 USER_CREATED 이벤트를 발행합니다.
     * 
     * @param auth0Id Auth0에서 제공하는 사용자 식별자
     * @param userEmail 사용자 이메일 주소
     * @return 생성된 사용자 정보
     * @throws IllegalArgumentException 입력 파라미터가 유효하지 않은 경우
     * @throws RuntimeException 사용자 생성 실패 시
     */
    public User createUser(String auth0Id, String userEmail) {
        log.info("새로운 사용자 생성 시작 - auth0Id: {}, email: {}", auth0Id, userEmail);
        
        // 입력 파라미터 유효성 검증
        validateCreateUserParameters(auth0Id, userEmail);
        
        // 중복 사용자 확인
        checkDuplicateUser(auth0Id, userEmail);
        
        try {
            // 새 사용자 엔티티 생성
            User newUser = User.builder()
                    .userId(UserIdGenerator.generateUserId())
                    .auth0Id(auth0Id)
                    .userEmail(userEmail)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            // 데이터베이스에 사용자 저장
            User savedUser = userRepository.save(newUser);
            log.info("사용자 생성 성공 - userId: {}, auth0Id: {}", savedUser.getUserId(), auth0Id);
            
            // 사용자 생성 이벤트 발행
            publishUserCreatedEvent(savedUser);
            
            return savedUser;
            
        } catch (Exception e) {
            log.error("사용자 생성 중 오류 발생 - auth0Id: {}, email: {}", auth0Id, userEmail, e);
            throw new RuntimeException("사용자 생성에 실패했습니다.", e);
        }
    }
    
    /**
     * 사용자 ID로 사용자 조회
     * 
     * @param userId 조회할 사용자의 고유 식별자
     * @return 사용자 정보, 존재하지 않으면 Optional.empty()
     * @throws IllegalArgumentException 사용자 ID가 유효하지 않은 경우
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(String userId) {
        log.debug("사용자 ID로 조회 시작 - userId: {}", userId);
        
        validateUserId(userId);
        
        Optional<User> user = userRepository.findById(userId);
        log.debug("사용자 ID로 조회 완료 - userId: {}, found: {}", userId, user.isPresent());
        
        return user;
    }
    
    /**
     * Auth0 ID로 사용자 조회
     * 
     * Auth0 JWT 토큰 검증 후 사용자 정보를 조회할 때 사용됩니다.
     * 
     * @param auth0Id Auth0에서 제공하는 사용자 식별자
     * @return 사용자 정보, 존재하지 않으면 Optional.empty()
     * @throws IllegalArgumentException Auth0 ID가 유효하지 않은 경우
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByAuth0Id(String auth0Id) {
        log.debug("Auth0 ID로 조회 시작 - auth0Id: {}", auth0Id);
        
        validateAuth0Id(auth0Id);
        
        Optional<User> user = userRepository.findByAuth0Id(auth0Id);
        log.debug("Auth0 ID로 조회 완료 - auth0Id: {}, found: {}", auth0Id, user.isPresent());
        
        return user;
    }
    
    /**
     * 이메일로 사용자 조회
     * 
     * @param userEmail 조회할 사용자의 이메일 주소
     * @return 사용자 정보, 존재하지 않으면 Optional.empty()
     * @throws IllegalArgumentException 이메일이 유효하지 않은 경우
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String userEmail) {
        log.debug("이메일로 조회 시작 - email: {}", userEmail);
        
        validateEmail(userEmail);
        
        Optional<User> user = userRepository.findByUserEmail(userEmail);
        log.debug("이메일로 조회 완료 - email: {}, found: {}", userEmail, user.isPresent());
        
        return user;
    }
    

    
    /**
     * 모든 사용자 조회
     * 
     * @return 전체 사용자 목록
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        log.debug("전체 사용자 조회 시작");
        
        List<User> users = userRepository.findAll();
        log.debug("전체 사용자 조회 완료 - count: {}", users.size());
        
        return users;
    }
    
    /**
     * 사용자 이메일 업데이트
     * 
     * @param userId 업데이트할 사용자의 ID
     * @param newEmail 새로운 이메일 주소
     * @return 업데이트된 사용자 정보, 사용자가 존재하지 않으면 Optional.empty()
     * @throws IllegalArgumentException 입력 파라미터가 유효하지 않은 경우
     */
    public Optional<User> updateUserEmail(String userId, String newEmail) {
        log.info("사용자 이메일 업데이트 시작 - userId: {}, newEmail: {}", userId, newEmail);
        
        validateUserId(userId);
        validateEmail(newEmail);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            log.warn("업데이트하려는 사용자가 존재하지 않음 - userId: {}", userId);
            return Optional.empty();
        }
        
        // 이메일 중복 확인
        if (userRepository.existsByUserEmail(newEmail)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + newEmail);
        }
        
        User user = userOptional.get();
        user.updateEmail(newEmail);
        
        User updatedUser = userRepository.save(user);
        log.info("사용자 이메일 업데이트 성공 - userId: {}, newEmail: {}", userId, newEmail);
        
        return Optional.of(updatedUser);
    }
    
    /**
     * 사용자 삭제
     * 
     * 사용자를 시스템에서 완전히 삭제합니다.
     * 삭제 성공 시 USER_DELETED 이벤트를 발행합니다.
     * 
     * @param userId 삭제할 사용자의 ID
     * @param deletionReason 삭제 사유 (선택적)
     * @return 삭제 성공 여부 (true: 성공, false: 사용자가 존재하지 않음)
     * @throws IllegalArgumentException 사용자 ID가 유효하지 않은 경우
     */
    public boolean deleteUser(String userId, String deletionReason) {
        log.info("사용자 삭제 시작 - userId: {}, reason: {}", userId, deletionReason);
        
        validateUserId(userId);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            log.warn("삭제하려는 사용자가 존재하지 않음 - userId: {}", userId);
            return false;
        }
        
        User user = userOptional.get();
        
        try {
            // 사용자 삭제
            userRepository.delete(user);
            log.info("사용자 삭제 성공 - userId: {}", userId);
            
            // 사용자 삭제 이벤트 발행
            publishUserDeletedEvent(user, deletionReason);
            
            return true;
            
        } catch (Exception e) {
            log.error("사용자 삭제 중 오류 발생 - userId: {}", userId, e);
            throw new RuntimeException("사용자 삭제에 실패했습니다.", e);
        }
    }
    
    /**
     * 사용자 존재 여부 확인 (Auth0 ID 기준)
     * 
     * @param auth0Id 확인할 Auth0 사용자 ID
     * @return 사용자가 존재하면 true, 그렇지 않으면 false
     * @throws IllegalArgumentException Auth0 ID가 유효하지 않은 경우
     */
    @Transactional(readOnly = true)
    public boolean existsByAuth0Id(String auth0Id) {
        log.debug("사용자 존재 여부 확인 (Auth0 ID) - auth0Id: {}", auth0Id);
        
        validateAuth0Id(auth0Id);
        
        boolean exists = userRepository.existsByAuth0Id(auth0Id);
        log.debug("사용자 존재 여부 확인 완료 - auth0Id: {}, exists: {}", auth0Id, exists);
        
        return exists;
    }
    
    /**
     * 사용자 존재 여부 확인 (이메일 기준)
     * 
     * @param userEmail 확인할 사용자 이메일
     * @return 사용자가 존재하면 true, 그렇지 않으면 false
     * @throws IllegalArgumentException 이메일이 유효하지 않은 경우
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String userEmail) {
        log.debug("사용자 존재 여부 확인 (이메일) - email: {}", userEmail);
        
        validateEmail(userEmail);
        
        boolean exists = userRepository.existsByUserEmail(userEmail);
        log.debug("사용자 존재 여부 확인 완료 - email: {}, exists: {}", userEmail, exists);
        
        return exists;
    }
    
    /**
     * 전체 사용자 수 조회
     * 
     * @return 시스템에 등록된 전체 사용자 수
     */
    @Transactional(readOnly = true)
    public long getUserCount() {
        long count = userRepository.countAllUsers();
        log.debug("전체 사용자 수 조회 완료 - count: {}", count);
        return count;
    }
    
    /**
     * 사용자 생성 파라미터 유효성 검증
     */
    private void validateCreateUserParameters(String auth0Id, String userEmail) {
        validateAuth0Id(auth0Id);
        validateEmail(userEmail);
    }
    
    /**
     * 중복 사용자 확인
     */
    private void checkDuplicateUser(String auth0Id, String userEmail) {
        if (userRepository.existsByAuth0Id(auth0Id)) {
            throw new IllegalArgumentException("이미 등록된 Auth0 사용자입니다: " + auth0Id);
        }
        if (userRepository.existsByUserEmail(userEmail)) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다: " + userEmail);
        }
    }
    
    /**
     * 사용자 ID 유효성 검증
     */
    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
    }
    
    /**
     * Auth0 ID 유효성 검증
     */
    private void validateAuth0Id(String auth0Id) {
        if (auth0Id == null || auth0Id.trim().isEmpty()) {
            throw new IllegalArgumentException("Auth0 ID는 필수입니다.");
        }
        
        // Auth0 ID 형식 검증 - 다양한 OAuth 제공자 지원
        if (!isValidAuth0IdFormat(auth0Id)) {
            throw new IllegalArgumentException("올바른 Auth0 ID 형식이 아닙니다. 지원 형식: provider|identifier (예: google-oauth2|123456789)");
        }
    }
    
    /**
     * Auth0 ID 형식 유효성 검증
     * 다양한 OAuth 제공자의 Auth0 ID 형식을 지원합니다.
     * 
     * 지원 형식:
     * - auth0|{identifier} (Auth0 네이티브 사용자)
     * - google-oauth2|{identifier} (구글 OAuth)
     * - github|{identifier} (깃허브 OAuth)
     * - facebook|{identifier} (페이스북 OAuth)
     * - twitter|{identifier} (트위터 OAuth)
     * - linkedin|{identifier} (링크드인 OAuth)
     * - apple|{identifier} (애플 OAuth)
     * - microsoft|{identifier} (마이크로소프트 OAuth)
     * 
     * @param auth0Id 검증할 Auth0 ID
     * @return 유효한 형식이면 true, 그렇지 않으면 false
     */
    private boolean isValidAuth0IdFormat(String auth0Id) {
        if (auth0Id == null || auth0Id.trim().isEmpty()) {
            return false;
        }
        
        // Auth0 ID는 "provider|identifier" 형식이어야 함
        if (!auth0Id.contains("|")) {
            return false;
        }
        
        String[] parts = auth0Id.split("\\|", 2);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            return false;
        }
        
        String provider = parts[0].toLowerCase();
        String identifier = parts[1];
        
        // 지원하는 OAuth 제공자 목록
        boolean isValidProvider = provider.equals("auth0") ||
                                provider.equals("google-oauth2") ||
                                provider.equals("github") ||
                                provider.equals("facebook") ||
                                provider.equals("twitter") ||
                                provider.equals("linkedin") ||
                                provider.equals("apple") ||
                                provider.equals("microsoft") ||
                                provider.equals("windowslive") ||
                                provider.equals("oauth2");
        
        // 기본 형식 검증: identifier는 최소 1자 이상이어야 함
        boolean isValidIdentifier = identifier.length() >= 1 && 
                                  identifier.matches("^[a-zA-Z0-9._-]+$");
        
        return isValidProvider && isValidIdentifier;
    }
    
    /**
     * 이메일 유효성 검증
     */
    private void validateEmail(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        if (!userEmail.contains("@")) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식입니다.");
        }
    }
    
    /**
     * 사용자 생성 이벤트 발행
     */
    private void publishUserCreatedEvent(User user) {
        try {
            UserCreatedEvent event = UserCreatedEvent.builder()
                    .userId(user.getUserId())
                    .auth0Id(user.getAuth0Id())
                    .userEmail(user.getUserEmail())
                    .createdAt(user.getCreatedAt())
                    .build();
            
            eventPublisher.publishEvent("user-service-events", event);
            log.info("사용자 생성 이벤트 발행 성공 - userId: {}", user.getUserId());
        } catch (Exception e) {
            log.warn("사용자 생성 이벤트 발행 실패 - userId: {}", user.getUserId(), e);
            // 이벤트 발행 실패는 전체 트랜잭션을 롤백시키지 않음
        }
    }
    
    /**
     * 사용자 삭제 이벤트 발행
     */
    private void publishUserDeletedEvent(User user, String deletionReason) {
        try {
            UserDeletedEvent event = UserDeletedEvent.builder()
                    .userId(user.getUserId())
                    .auth0Id(user.getAuth0Id())
                    .userEmail(user.getUserEmail())
                    .deletedAt(LocalDateTime.now())
                    .deletionReason(deletionReason)
                    .build();
            
            eventPublisher.publishEvent("user-service-events", event);
            log.info("사용자 삭제 이벤트 발행 성공 - userId: {}", user.getUserId());
        } catch (Exception e) {
            log.warn("사용자 삭제 이벤트 발행 실패 - userId: {}", user.getUserId(), e);
            // 이벤트 발행 실패는 전체 트랜잭션을 롤백시키지 않음
        }
    }
}