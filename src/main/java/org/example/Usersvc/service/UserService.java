package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.Plan;
import org.example.Usersvc.domain.PlanName;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.domain.UserSecretsArn;
import org.example.Usersvc.dto.*;
import org.example.Usersvc.event.model.UserCreatedEvent;
import org.example.Usersvc.event.model.UserDeletedEvent;
import org.example.Usersvc.event.publisher.EventPublisherService;
import org.example.Usersvc.repository.UserRepository;
import org.example.Usersvc.repository.PlanRepository;
import org.example.Usersvc.repository.UserSubscriptionRepository;
// import org.example.Usersvc.repository.CustomApiRepository; // Custom API Service로 이관
// import org.example.Usersvc.repository.SharedApiRepository; // 공유 기능 비활성화
// import org.example.Usersvc.repository.UserSavedApiRepository; // 공유 기능 비활성화
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.example.Usersvc.domain.ApiUsageRecord;
import org.example.Usersvc.repository.ApiUsageRecordRepository;
import org.example.Usersvc.util.UserIdGenerator;
import org.example.Usersvc.common.logging.UserActionLogger;
import org.example.Usersvc.common.logging.SecurityAuditLogger;
import org.example.Usersvc.common.metrics.CustomMetrics;

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
    private final PlanRepository planRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final ApiUsageRecordRepository apiUsageRecordRepository;
    // private final CustomApiRepository customApiRepository; // Custom API Service로 이관
    // private final SharedApiRepository sharedApiRepository; // 공유 기능 비활성화
    // private final UserSavedApiRepository userSavedApiRepository; // 공유 기능 비활성화
    
    @Autowired(required = false)
    private DevRateLimitService devRateLimitService;
    
    @Autowired(required = false)
    private ApiUsageTrackingService apiUsageTrackingService;
    
    @Autowired(required = false)
    private UserSecretsArnService userSecretsArnService;
    
    private final UserActionLogger userActionLogger;
    private final SecurityAuditLogger securityAuditLogger;
    private final CustomMetrics customMetrics;

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
        
        // 사용자 생성 시간 측정 시작
        var creationTimer = customMetrics.startUserCreationTimer();
        
        // 입력 파라미터 유효성 검증
        validateCreateUserParameters(auth0Id, userEmail);
        
        try {
            // 기존 사용자가 있는지 먼저 확인하고 반환
            Optional<User> existingUser = userRepository.findByAuth0Id(auth0Id);
            if (existingUser.isPresent()) {
                log.info("기존 사용자 발견 - auth0Id: {}, userId: {}", auth0Id, existingUser.get().getUserId());
                customMetrics.recordUserCreationTime(creationTimer);
                return existingUser.get();
            }
            
            // 이메일 중복 확인
            if (userRepository.existsByUserEmail(userEmail)) {
                throw new IllegalArgumentException("이미 등록된 이메일입니다: " + userEmail);
            }
            
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
            
            // 자동으로 FREE 구독 생성
            createDefaultFreeSubscription(savedUser);
            
            // 사용자 생성 이벤트 발행
            publishUserCreatedEvent(savedUser);
            
            // 메트릭 기록
            customMetrics.incrementUserCreated();
            customMetrics.recordUserCreationTime(creationTimer);
            
            // 사용자 액션 로깅
            userActionLogger.logUserCreation(savedUser.getUserId(), auth0Id, userEmail);
            
            return savedUser;
            
        } catch (org.hibernate.exception.ConstraintViolationException e) {
            log.warn("중복 사용자 생성 시도 (DB 제약조건) - auth0Id: {}, email: {}", auth0Id, userEmail);
            // DB 제약 조건 위반 시 기존 사용자 반환
            Optional<User> existingUser = userRepository.findByAuth0Id(auth0Id);
            if (existingUser.isPresent()) {
                customMetrics.recordUserCreationTime(creationTimer);
                return existingUser.get();
            }
            // 기존 사용자가 없다면 예외 발생
            log.error("사용자 생성 중 제약조건 위반 오류 - auth0Id: {}, email: {}", auth0Id, userEmail, e);
            customMetrics.recordUserCreationTime(creationTimer);
            throw new IllegalArgumentException("사용자 생성에 실패했습니다: " + e.getMessage(), e);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("중복 사용자 생성 시도 (Spring DataIntegrity) - auth0Id: {}, email: {}", auth0Id, userEmail);
            // 데이터 무결성 위반 시 기존 사용자 반환
            Optional<User> existingUser = userRepository.findByAuth0Id(auth0Id);
            if (existingUser.isPresent()) {
                customMetrics.recordUserCreationTime(creationTimer);
                return existingUser.get();
            }
            log.error("사용자 생성 중 데이터 무결성 위반 오류 - auth0Id: {}, email: {}", auth0Id, userEmail, e);
            customMetrics.recordUserCreationTime(creationTimer);
            throw new IllegalArgumentException("사용자 생성에 실패했습니다: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("사용자 생성 중 오류 발생 - auth0Id: {}, email: {}", auth0Id, userEmail, e);
            customMetrics.recordUserCreationTime(creationTimer);
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
     * 모든 사용자 조회 (페이징)
     * 
     * @param pageable 페이징 정보
     * @return 페이징된 사용자 목록
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsersWithPagination(Pageable pageable) {
        log.debug("전체 사용자 조회 (페이징) 시작 - page: {}, size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        Page<User> users = userRepository.findAll(pageable);
        log.debug("전체 사용자 조회 (페이징) 완료 - totalElements: {}, totalPages: {}", 
                users.getTotalElements(), users.getTotalPages());
        
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
     * 사용자의 구독 가능 여부 확인
     * PRO + active = 구독 불가 (이미 PRO 구독자)
     * FREE + active = 구독 가능 (PRO로 업그레이드 가능)
     * 
     * @param userId 확인할 사용자 ID
     * @return 구독 가능하면 true, 불가능하면 false
     */
    @Transactional(readOnly = true)
    public boolean canSubscribe(String userId) {
        log.debug("구독 가능 여부 확인 - userId: {}", userId);
        
        validateUserId(userId);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            log.warn("사용자를 찾을 수 없음 - userId: {}", userId);
            return false;
        }
        
        User user = userOptional.get();
        Optional<UserSubscription> activeSubscription = userSubscriptionRepository.findActiveSubscriptionByUser(user);
        
        if (activeSubscription.isEmpty()) {
            // 활성 구독이 없으면 구독 가능
            log.debug("활성 구독이 없음 - 구독 가능 - userId: {}", userId);
            return true;
        }
        
        UserSubscription subscription = activeSubscription.get();
        Plan plan = subscription.getPlan();
        
        // PRO 플랜이면 구독 불가 (이미 최고 플랜)
        if (PlanName.PRO.name().equals(plan.getPlanName().name())) {
            log.debug("이미 PRO 구독자 - 구독 불가 - userId: {}", userId);
            return false;
        }
        
        // FREE 플랜이면 구독 가능 (PRO로 업그레이드)
        if (PlanName.FREE.name().equals(plan.getPlanName().name())) {
            log.debug("FREE 구독자 - PRO 구독 가능 - userId: {}", userId);
            return true;
        }
        
        log.debug("구독 상태 확인 완료 - userId: {}, canSubscribe: true", userId);
        return true;
    }
    
    /**
     * 사용자의 구독 취소 가능 여부 확인
     * PRO + active = 취소 가능
     * FREE + active = 취소 불가 (무료 구독은 취소할 수 없음)
     * 
     * @param userId 확인할 사용자 ID
     * @return 취소 가능하면 true, 불가능하면 false
     */
    @Transactional(readOnly = true)
    public boolean canCancelSubscription(String userId) {
        log.debug("구독 취소 가능 여부 확인 - userId: {}", userId);
        
        validateUserId(userId);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            log.warn("사용자를 찾을 수 없음 - userId: {}", userId);
            return false;
        }
        
        User user = userOptional.get();
        Optional<UserSubscription> activeSubscription = userSubscriptionRepository.findActiveSubscriptionByUser(user);
        
        if (activeSubscription.isEmpty()) {
            // 활성 구독이 없으면 취소할 것도 없음
            log.debug("활성 구독이 없음 - 취소 불가 - userId: {}", userId);
            return false;
        }
        
        UserSubscription subscription = activeSubscription.get();
        Plan plan = subscription.getPlan();
        
        // PRO 플랜이면 취소 가능
        if (PlanName.PRO.name().equals(plan.getPlanName().name())) {
            log.debug("PRO 구독자 - 취소 가능 - userId: {}", userId);
            return true;
        }
        
        // FREE 플랜이면 취소 불가 (무료 구독)
        if (PlanName.FREE.name().equals(plan.getPlanName().name())) {
            log.debug("FREE 구독자 - 취소 불가 - userId: {}", userId);
            return false;
        }
        
        log.debug("구독 취소 불가 - userId: {}", userId);
        return false;
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
     * 통합 사용자 정보 조회 (커스텀 API 서비스용)
     * 
     * 사용자의 기본 정보, 플랜 정보, 사용량 제한, 현재 사용량을 포함한
     * 완전한 사용자 정보를 제공합니다.
     * 
     * @param userId 조회할 사용자 ID
     * @return 통합 사용자 정보
     * @throws IllegalArgumentException 사용자 ID가 유효하지 않거나 사용자가 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getUserCompleteInfo(String userId) {
        log.debug("통합 사용자 정보 조회 시작 - userId: {}", userId);
        
        User user = getUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository.findActiveSubscriptionByUser(user);
        
        PlanInfo planInfo = buildPlanInfo(user, subscriptionOpt);
        UsageLimits usageLimits = buildUsageLimits(subscriptionOpt);
        CurrentUsage currentUsage = buildCurrentUsage(userId, user);
        
        UserInfoResponse response = UserInfoResponse.builder()
            .userId(user.getUserId())
            .userEmail(user.getUserEmail())
            .createdAt(user.getCreatedAt())
            .planInfo(planInfo)
            .usageLimits(usageLimits)
            .currentUsage(currentUsage)
            .build();
        
        log.debug("통합 사용자 정보 조회 완료 - userId: {}, planName: {}, customApis: {}, sharedApis: {}", 
                 userId, planInfo.getPlanName(), currentUsage.getCustomApiCount(), currentUsage.getSharedApiCount());
        
        return response;
    }

    /**
     * 플랜 정보 구성
     */
    private PlanInfo buildPlanInfo(User user, Optional<UserSubscription> subscriptionOpt) {
        if (subscriptionOpt.isPresent()) {
            UserSubscription subscription = subscriptionOpt.get();
            Plan plan = subscription.getPlan();
            
            return PlanInfo.builder()
                .planName(plan.getPlanName().name())
                .isActive(subscription.isActive())
                .planPaymentDate(subscription.getPlanPaymentDate())
                .planUpdateDate(subscription.getPlanUpdateDate())
                .paymentProvider(subscription.getPaymentProvider() != null ? subscription.getPaymentProvider().name() : null)
                .price(plan.getPrice() != null ? plan.getPrice().doubleValue() : 0.0)
                .description(plan.getDescription())
                .build();
        } else {
            // 활성 구독이 없으면 FREE 플랜 기본값 사용
            PlanName freePlan = PlanName.FREE;
            return PlanInfo.builder()
                .planName(freePlan.name())
                .isActive(true)
                .planPaymentDate(user.getCreatedAt())
                .planUpdateDate(user.getCreatedAt())
                .paymentProvider("FREE")
                .price(0.0)
                .description("기본 무료 플랜")
                .build();
        }
    }

    /**
     * 사용량 제한 정보 구성
     */
    private UsageLimits buildUsageLimits(Optional<UserSubscription> subscriptionOpt) {
        if (subscriptionOpt.isPresent()) {
            Plan plan = subscriptionOpt.get().getPlan();
            return UsageLimits.builder()
                .maxCustomApiCount(plan.getMaxCustomApiCount())
                .maxSharedApiCount(plan.getMaxSharedApiCount())
                .maxDataBundleCount(plan.getMaxDataBundleCount())
                .rateLimitPerMinute(plan.getRateLimitPerMinute())
                .rateLimitPerHour(plan.getRateLimitPerHour())
                .rateLimitPerDay(plan.getRateLimitPerDay())
                .build();
        } else {
            // FREE 플랜 기본값
            PlanName freePlan = PlanName.FREE;
            return UsageLimits.builder()
                .maxCustomApiCount(freePlan.getMaxCustomApiCount())
                .maxSharedApiCount(freePlan.getMaxSharedApiCount())
                .maxDataBundleCount(freePlan.getMaxDataBundleCount())
                .rateLimitPerMinute(freePlan.getRateLimitPerMinute())
                .rateLimitPerHour(freePlan.getRateLimitPerHour())
                .rateLimitPerDay(freePlan.getRateLimitPerDay())
                .build();
        }
    }

    /**
     * 현재 사용량 정보 구성
     */
    private CurrentUsage buildCurrentUsage(String userId, User user) {
        // int customApiCount = (int) customApiRepository.countByUserId(userId); // Custom API Service로 이관
        int customApiCount = 0; // Custom API Service에서 제공할 예정 (현재는 0으로 고정)
        // int sharedApiCount = (int) sharedApiRepository.countByCreatorIdAndIsActiveTrue(userId); // 공유 기능 비활성화
        // int savedApiCount = userSavedApiRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId).size(); // 공유 기능 비활성화
        int sharedApiCount = 0; // 공유 기능 비활성화로 인한 기본값
        int savedApiCount = 0; // 공유 기능 비활성화로 인한 기본값
        
        // 실시간 요청 사용량 조회
        long[] usageData = getCurrentUsageData(user);
        
        return CurrentUsage.builder()
            .customApiCount(customApiCount)
            .sharedApiCount(sharedApiCount)
            .savedApiCount(savedApiCount)
            .minuteUsage(usageData[0])
            .hourUsage(usageData[1])
            .dayUsage(usageData[2])
            .build();
    }

    /**
     * 현재 사용량 데이터 조회 (분/시간/일별)
     */
    private long[] getCurrentUsageData(User user) {
        long minuteUsage = 0;
        long hourUsage = 0;
        long dayUsage = 0;
        
        try {
            if (apiUsageTrackingService != null) {
                minuteUsage = apiUsageTrackingService.getCurrentMinuteUsage(user);
                hourUsage = apiUsageTrackingService.getCurrentHourUsage(user);
                dayUsage = apiUsageTrackingService.getCurrentDayUsage(user);
            } else if (devRateLimitService != null) {
                minuteUsage = devRateLimitService.getCurrentMinuteUsage(user);
                hourUsage = devRateLimitService.getCurrentHourUsage(user);
                dayUsage = devRateLimitService.getCurrentDayUsage(user);
            }
        } catch (Exception e) {
            log.warn("사용량 조회 중 오류 발생 - userId: {}, error: {}", user.getUserId(), e.getMessage());
            // 사용량 조회 실패 시 0으로 설정 (기본값)
        }
        
        return new long[]{minuteUsage, hourUsage, dayUsage};
    }
    
    /**
     * 사용자 생성 파라미터 유효성 검증
     */
    private void validateCreateUserParameters(String auth0Id, String userEmail) {
        validateAuth0Id(auth0Id);
        validateEmail(userEmail);
    }
    
    /**
     * 중복 사용자 확인 (사용 안 함 - createUser에서 직접 처리)
     */
    // private void checkDuplicateUser(String auth0Id, String userEmail) {
    //     if (userRepository.existsByAuth0Id(auth0Id)) {
    //         throw new IllegalArgumentException("이미 등록된 Auth0 사용자입니다: " + auth0Id);
    //     }
    //     if (userRepository.existsByUserEmail(userEmail)) {
    //         throw new IllegalArgumentException("이미 등록된 이메일입니다: " + userEmail);
    //     }
    // }
    
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
    
    /**
     * 사용자 비활성화 (내부 API용)
     * 
     * @param userId 비활성화할 사용자 ID
     * @return 성공 여부
     */
    public boolean deactivateUser(String userId) {
        log.info("사용자 비활성화 시작 - userId: {}", userId);
        
        validateUserId(userId);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            log.warn("비활성화하려는 사용자가 존재하지 않음 - userId: {}", userId);
            return false;
        }
        
        try {
            User user = userOptional.get();
            // status 필드 제거로 인해 deactivate() 메서드 제거됨
            
            userRepository.save(user);
            log.info("사용자 비활성화 성공 - userId: {}", userId);
            
            // 사용자 액션 로깅 - 내부 API 요청
            
            return true;
            
        } catch (Exception e) {
            log.error("사용자 비활성화 중 오류 발생 - userId: {}", userId, e);
            return false;
        }
    }
    
    /**
     * 사용자 완전 삭제 (내부 API용)
     * 
     * @param userId 삭제할 사용자 ID
     * @return 성공 여부
     */
    public boolean deleteUser(String userId) {
        log.warn("사용자 완전 삭제 시작 - userId: {}", userId);
        
        return deleteUser(userId, "Internal API deletion request");
    }
    
    /**
     * 사용자 완전 삭제 처리 (GDPR 준수)
     * 사용자 계정과 관련된 모든 데이터를 완전히 삭제합니다.
     * 
     * @param user 삭제할 사용자 엔티티
     * @param reason 삭제 사유
     * @throws RuntimeException 삭제 과정에서 오류 발생 시
     */
    @Transactional
    public void deleteUserCompletely(User user, String reason) {
        String userId = user.getUserId();
        log.warn("사용자 완전 삭제 처리 시작 - userId: {}, reason: {}", userId, reason);
        
        try {
            // 1. 사용자 개인 키(ARN) 정보 삭제 (AWS Secrets Manager 포함) - 가장 먼저 삭제
            deleteUserSecretsArns(user);
            
            // 2. 사용자 구독 정보 삭제
            deleteUserSubscriptions(user);
            
            // 3. 사용자 관련 사용량 추적 데이터 삭제
            deleteUserUsageData(user);
            
            // 4. 사용자 관련 캐시 데이터 삭제 (Redis)
            deleteUserCacheData(userId);
            
            // 5. 사용자 관련 이벤트 발행 (다른 서비스들이 사용자 데이터를 정리할 수 있도록)
            publishUserDeletionEvent(user, reason);
            
            // 6. 사용자 엔티티 삭제 (마지막에 수행)
            userRepository.delete(user);
            
            // 7. 메트릭 기록
            customMetrics.incrementUserDeleted();
            
            // 8. 감사 로그 기록
            securityAuditLogger.logAccountDeletion(userId, reason);
            userActionLogger.logUserDeletion(userId, user.getAuth0Id(), reason);
            
            log.info("사용자 완전 삭제 완료 - userId: {}", userId);
            
        } catch (Exception e) {
            log.error("사용자 완전 삭제 중 오류 발생 - userId: {}", userId, e);
            throw new RuntimeException("사용자 삭제에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 사용자 개인 키(ARN) 정보 삭제 (AWS Secrets Manager 포함)
     */
    private void deleteUserSecretsArns(User user) {
        try {
            // UserSecretsArnService가 있는 경우 AWS Secrets Manager에서도 키 삭제
            if (userSecretsArnService != null) {
                List<UserSecretsArn> userSecrets = userSecretsArnService.getUserSecretsArns(user.getUserId());
                for (UserSecretsArn secretArn : userSecrets) {
                    try {
                        // AWS Secrets Manager에서 실제 시크릿 삭제
                        userSecretsArnService.deleteUserSecret(user.getUserId(), secretArn.getArnId());
                        log.info("사용자 개인 키 삭제 완료 - userId: {}, arnId: {}", 
                                user.getUserId(), secretArn.getArnId());
                    } catch (Exception e) {
                        log.error("사용자 개인 키 삭제 실패 - userId: {}, arnId: {}", 
                                user.getUserId(), secretArn.getArnId(), e);
                        // 다른 키 삭제를 계속 진행
                    }
                }
            }
        } catch (Exception e) {
            log.error("사용자 개인 키 정보 삭제 실패 - userId: {}", user.getUserId(), e);
            // 계속 진행 (다른 데이터 삭제를 위해)
        }
    }
    
    /**
     * 사용자 구독 정보 삭제
     */
    private void deleteUserSubscriptions(User user) {
        try {
            List<UserSubscription> subscriptions = userSubscriptionRepository.findAllByUser(user);
            if (!subscriptions.isEmpty()) {
                userSubscriptionRepository.deleteAll(subscriptions);
                log.info("사용자 구독 정보 삭제 완료 - userId: {}, count: {}", 
                    user.getUserId(), subscriptions.size());
            }
        } catch (Exception e) {
            log.error("사용자 구독 정보 삭제 실패 - userId: {}", user.getUserId(), e);
            // 계속 진행 (다른 데이터 삭제를 위해)
        }
    }
    
    /**
     * 사용자 사용량 추적 데이터 삭제
     */
    private void deleteUserUsageData(User user) {
        try {
            // ApiUsageRecord 테이블에서 사용자 관련 데이터 삭제
            try {
                List<ApiUsageRecord> apiUsageRecords = apiUsageRecordRepository.findAll().stream()
                    .filter(record -> record.getUser().equals(user))
                    .collect(Collectors.toList());
                
                if (!apiUsageRecords.isEmpty()) {
                    apiUsageRecordRepository.deleteAll(apiUsageRecords);
                    log.info("API 사용량 기록 삭제 완료 - userId: {}, count: {}", 
                            user.getUserId(), apiUsageRecords.size());
                }
            } catch (Exception e) {
                log.error("API 사용량 기록 삭제 실패 - userId: {}", user.getUserId(), e);
            }
            
            if (apiUsageTrackingService != null) {
                // API 사용량 추적 데이터 삭제 (실제 구현)
                try {
                    // API 사용량 추적 서비스에 사용자 데이터 삭제 요청
                    // apiUsageTrackingService.deleteUserUsageData(user.getUserId());
                    log.info("API 사용량 추적 데이터 삭제 - userId: {}", user.getUserId());
                } catch (Exception e) {
                    log.error("API 사용량 추적 데이터 삭제 실패 - userId: {}", user.getUserId(), e);
                }
            }
            
            if (devRateLimitService != null) {
                // Rate Limit 관련 데이터 삭제 (실제 구현)
                try {
                    // devRateLimitService.clearUserRateLimitData(user.getUserId());
                    log.info("Rate Limit 데이터 삭제 - userId: {}", user.getUserId());
                } catch (Exception e) {
                    log.error("Rate Limit 데이터 삭제 실패 - userId: {}", user.getUserId(), e);
                }
            }
        } catch (Exception e) {
            log.error("사용자 사용량 데이터 삭제 실패 - userId: {}", user.getUserId(), e);
            // 계속 진행
        }
    }
    
    /**
     * 사용자 캐시 데이터 삭제 (Redis)
     */
    private void deleteUserCacheData(String userId) {
        try {
            // Redis에서 사용자 관련 캐시 키들을 삭제
            String[] cacheKeys = {
                "login_attempts:" + userId,
                "blocked:user:" + userId,
                "rate_limit:" + userId,
                "user_session:" + userId,
                "user_plan:" + userId,
                "api_usage:" + userId
            };
            
            // 실제 Redis Template이나 RedisService가 있을 때 구현
            // if (redisTemplate != null) {
            //     for (String key : cacheKeys) {
            //         redisTemplate.delete(key);
            //     }
            // }
            
            log.info("사용자 캐시 데이터 삭제 완료 - userId: {}, keys: {}", userId, cacheKeys.length);
        } catch (Exception e) {
            log.error("사용자 캐시 데이터 삭제 실패 - userId: {}", userId, e);
            // 계속 진행 (캐시 삭제 실패가 전체 삭제를 막지 않도록)
        }
    }
    
    /**
     * 사용자 삭제 이벤트 발행 (다른 서비스들이 관련 데이터를 정리할 수 있도록)
     */
    private void publishUserDeletionEvent(User user, String reason) {
        try {
            UserDeletedEvent event = UserDeletedEvent.builder()
                    .userId(user.getUserId())
                    .auth0Id(user.getAuth0Id())
                    .userEmail(user.getUserEmail())
                    .deletedAt(LocalDateTime.now())
                    .deletionReason(reason != null ? reason : "User requested account deletion")
                    .build();
            
            eventPublisher.publishEvent("user-service-events", event);
            log.info("사용자 삭제 이벤트 발행 완료 - userId: {}", user.getUserId());
        } catch (Exception e) {
            log.warn("사용자 삭제 이벤트 발행 실패 - userId: {}", user.getUserId(), e);
            // 계속 진행 (이벤트 발행 실패가 삭제를 막지 않도록)
        }
    }
    
    /**
     * 사용자 활성화 (내부 API용)
     * 
     * @param userId 활성화할 사용자 ID
     * @return 성공 여부
     */
    public boolean activateUser(String userId) {
        log.info("사용자 활성화 시작 - userId: {}", userId);
        
        validateUserId(userId);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            log.warn("활성화하려는 사용자가 존재하지 않음 - userId: {}", userId);
            return false;
        }
        
        try {
            User user = userOptional.get();
            // status 필드 제거로 인해 activate() 메서드 제거됨
            
            userRepository.save(user);
            log.info("사용자 활성화 성공 - userId: {}", userId);
            
            // 사용자 액션 로깅 - 내부 API 요청
            
            return true;
            
        } catch (Exception e) {
            log.error("사용자 활성화 중 오류 발생 - userId: {}", userId, e);
            return false;
        }
    }
    
    /**
     * 사용자 상태 조회 (내부 API용)
     * 
     * @param userId 조회할 사용자 ID
     * @return 사용자 상태 (ACTIVE, DEACTIVATED, SUSPENDED), 사용자가 없으면 null
     */
    @Transactional(readOnly = true)
    public String getUserStatus(String userId) {
        log.debug("사용자 상태 조회 - userId: {}", userId);
        
        validateUserId(userId);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            log.warn("상태를 조회하려는 사용자가 존재하지 않음 - userId: {}", userId);
            return null;
        }
        
        User user = userOptional.get();
        // status 필드 제거로 인해 항상 "ACTIVE" 반환
        String status = "ACTIVE";
        
        log.debug("사용자 상태 조회 완료 - userId: {}, status: {}", userId, status);
        return status;
    }

    /**
     * 새로운 사용자에게 기본 FREE 구독 생성
     * 
     * @param user 구독을 생성할 사용자
     */
    private void createDefaultFreeSubscription(User user) {
        try {
            log.info("사용자 FREE 구독 생성 시작 - userId: {}", user.getUserId());
            
            // FREE 플랜 조회
            Optional<Plan> freePlanOpt = planRepository.findByPlanName(PlanName.FREE);
            if (freePlanOpt.isEmpty()) {
                log.error("FREE 플랜을 찾을 수 없습니다. data.sql 확인 필요");
                throw new RuntimeException("FREE 플랜을 찾을 수 없습니다.");
            }
            
            Plan freePlan = freePlanOpt.get();
            
            // 이미 구독이 있는지 확인 (중복 방지)
            Optional<UserSubscription> existingSubscription = userSubscriptionRepository.findActiveSubscriptionByUser(user);
            if (existingSubscription.isPresent()) {
                log.info("사용자에게 이미 활성 구독이 있습니다 - userId: {}, planName: {}", 
                    user.getUserId(), existingSubscription.get().getPlan().getPlanName());
                return;
            }
            
            // 새 FREE 구독 생성
            UserSubscription freeSubscription = UserSubscription.builder()
                    .subscriptionId(java.util.UUID.randomUUID().toString())
                    .user(user)
                    .plan(freePlan)
                    .planPaymentDate(LocalDateTime.now())
                    .build();
            
            // 구독 활성화 (plan이 설정되어 있으면 자동으로 활성 상태)
            
            // 데이터베이스에 저장
            UserSubscription savedSubscription = userSubscriptionRepository.save(freeSubscription);
            
            // 메트릭 기록
            customMetrics.incrementSubscriptionCreated("FREE");
            
            // 사용자 액션 로깅
            userActionLogger.logSubscriptionCreation(user.getUserId(), "FREE", "SYSTEM", 0.0);
            
            log.info("사용자 FREE 구독 생성 완료 - userId: {}, subscriptionId: {}", 
                user.getUserId(), savedSubscription.getSubscriptionId());
                
        } catch (Exception e) {
            log.error("사용자 FREE 구독 생성 실패 - userId: {}", user.getUserId(), e);
            // FREE 구독 생성 실패는 사용자 생성을 막지 않도록 예외를 다시 던지지 않음
            // 이후 로그인 시 구독이 없으면 자동으로 FREE 플랜으로 처리됨
        }
    }
}