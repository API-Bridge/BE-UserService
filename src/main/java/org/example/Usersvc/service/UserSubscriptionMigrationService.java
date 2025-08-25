package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.Plan;
import org.example.Usersvc.domain.PlanName;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.repository.PlanRepository;
import org.example.Usersvc.repository.UserRepository;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 구독 마이그레이션 서비스
 * 
 * 기존 사용자들에게 FREE 구독을 자동으로 생성하는 등의 
 * 데이터 마이그레이션 작업을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserSubscriptionMigrationService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    /**
     * 구독이 없는 모든 사용자에게 FREE 구독 생성
     * 
     * @return 처리된 사용자 수
     */
    public int createMissingFreeSubscriptions() {
        log.info("구독이 없는 사용자들에게 FREE 구독 자동 생성 시작");
        
        try {
            // FREE 플랜 조회
            Optional<Plan> freePlanOpt = planRepository.findByPlanName(PlanName.FREE);
            if (freePlanOpt.isEmpty()) {
                log.error("FREE 플랜을 찾을 수 없습니다. data.sql 확인 필요");
                throw new RuntimeException("FREE 플랜을 찾을 수 없습니다.");
            }
            
            Plan freePlan = freePlanOpt.get();
            
            // 모든 사용자 조회
            List<User> allUsers = userRepository.findAll();
            log.info("전체 사용자 수: {}", allUsers.size());
            
            int processedCount = 0;
            
            for (User user : allUsers) {
                // 이미 활성 구독이 있는지 확인
                Optional<UserSubscription> existingSubscription = 
                    userSubscriptionRepository.findActiveSubscriptionByUser(user);
                
                if (existingSubscription.isEmpty()) {
                    // 구독이 없는 사용자에게 FREE 구독 생성
                    createFreeSubscriptionForUser(user, freePlan);
                    processedCount++;
                } else {
                    log.debug("사용자 {}는 이미 활성 구독이 있습니다: {}", 
                        user.getUserId(), existingSubscription.get().getPlan().getPlanName());
                }
            }
            
            log.info("FREE 구독 자동 생성 완료 - 처리된 사용자 수: {}/{}", processedCount, allUsers.size());
            return processedCount;
            
        } catch (Exception e) {
            log.error("FREE 구독 자동 생성 중 오류 발생", e);
            throw new RuntimeException("FREE 구독 자동 생성에 실패했습니다.", e);
        }
    }
    
    /**
     * 특정 사용자에게 FREE 구독 생성
     * 
     * @param user 사용자
     * @param freePlan FREE 플랜
     */
    private void createFreeSubscriptionForUser(User user, Plan freePlan) {
        try {
            UserSubscription freeSubscription = UserSubscription.builder()
                    .subscriptionId(java.util.UUID.randomUUID().toString())
                    .user(user)
                    .plan(freePlan)
                    .planPaymentDate(LocalDateTime.now())
                    .build();
            
            // 구독 활성화
            
            // 데이터베이스에 저장
            UserSubscription savedSubscription = userSubscriptionRepository.save(freeSubscription);
            
            log.info("사용자 {}에게 FREE 구독 생성 완료 - subscriptionId: {}", 
                user.getUserId(), savedSubscription.getSubscriptionId());
                
        } catch (Exception e) {
            log.error("사용자 {}에게 FREE 구독 생성 실패", user.getUserId(), e);
            // 개별 사용자 실패는 전체 처리를 중단시키지 않음
        }
    }
    
    /**
     * 구독이 없는 사용자 수 조회
     * 
     * @return 구독이 없는 사용자 수
     */
    @Transactional(readOnly = true)
    public long countUsersWithoutSubscription() {
        List<User> allUsers = userRepository.findAll();
        long count = allUsers.stream()
            .mapToLong(user -> {
                Optional<UserSubscription> subscription = 
                    userSubscriptionRepository.findActiveSubscriptionByUser(user);
                return subscription.isEmpty() ? 1 : 0;
            })
            .sum();
            
        log.info("구독이 없는 사용자 수: {}", count);
        return count;
    }
}
