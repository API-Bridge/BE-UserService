package org.example.Usersvc.controller;

import org.example.Usersvc.domain.Plan;
import org.example.Usersvc.domain.PlanType;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.repository.PlanRepository;
import org.example.Usersvc.repository.UserRepository;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 수정된 구독 로직의 통합 테스트
 * 기존 레코드 업데이트 방식이 올바르게 작동하는지 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SubscriptionControllerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;

    private User testUser;
    private Plan freePlan;
    private Plan proPlan;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = new User("integration-test-user", "auth0|integrationtest", "integration@test.com");
        userRepository.save(testUser);

        // 테스트 플랜 생성
        freePlan = Plan.builder()
                .planType(PlanType.FREE)
                .price(BigDecimal.ZERO)
                .description("Free Plan for Integration Test")
                .build();
        planRepository.save(freePlan);

        proPlan = Plan.builder()
                .planType(PlanType.PRO)
                .price(BigDecimal.valueOf(22.00))
                .description("Pro Plan for Integration Test")
                .build();
        planRepository.save(proPlan);
    }

    @Test
    @DisplayName("신규 사용자의 첫 구독은 새 레코드 생성")
    void shouldCreateNewSubscriptionForNewUser() {
        // When: 신규 사용자가 PRO 구독을 시작
        UserSubscription newSubscription = UserSubscription.builder()
                .subscriptionId("integration-sub-001")
                .user(testUser)
                .plan(proPlan)
                .planPaymentDate(LocalDateTime.now())
                .build();
        newSubscription.setIsActive(true);
        newSubscription.setBillingKey("test-billing-key");
        
        userSubscriptionRepository.save(newSubscription);

        // Then: 새 구독 레코드가 생성되고 활성화됨
        Optional<UserSubscription> savedSub = userSubscriptionRepository.findActiveSubscriptionByUser(testUser);
        assertThat(savedSub).isPresent();
        assertThat(savedSub.get().getPlan().getPlanType()).isEqualTo(PlanType.PRO);
        assertThat(savedSub.get().getIsActive()).isTrue();
        assertThat(savedSub.get().getBillingKey()).isEqualTo("test-billing-key");
        
        // 전체 구독 레코드 수 확인
        long totalSubs = userSubscriptionRepository.countSubscriptionsByUser(testUser);
        assertThat(totalSubs).isEqualTo(1L);
    }

    @Test
    @DisplayName("기존 FREE 구독을 PRO로 업그레이드 시 같은 레코드 업데이트")
    void shouldUpdateExistingSubscriptionWhenUpgrading() {
        // Given: 사용자가 이미 FREE 구독을 가지고 있음
        UserSubscription freeSubscription = UserSubscription.builder()
                .subscriptionId("integration-sub-002")
                .user(testUser)
                .plan(freePlan)
                .planPaymentDate(LocalDateTime.now().minusDays(10))
                .build();
        freeSubscription.setIsActive(true);
        userSubscriptionRepository.save(freeSubscription);

        String originalSubscriptionId = freeSubscription.getSubscriptionId();

        // When: PRO로 업그레이드 (기존 레코드 업데이트)
        freeSubscription.setPlan(proPlan);
        freeSubscription.setBillingKey("pro-billing-key");
        freeSubscription.setPlanUpdateDate(LocalDateTime.now());
        userSubscriptionRepository.save(freeSubscription);

        // Then: 같은 구독 ID로 PRO 플랜이 활성화됨
        Optional<UserSubscription> updatedSub = userSubscriptionRepository.findActiveSubscriptionByUser(testUser);
        assertThat(updatedSub).isPresent();
        assertThat(updatedSub.get().getSubscriptionId()).isEqualTo(originalSubscriptionId); // 같은 ID
        assertThat(updatedSub.get().getPlan().getPlanType()).isEqualTo(PlanType.PRO);
        assertThat(updatedSub.get().getBillingKey()).isEqualTo("pro-billing-key");
        assertThat(updatedSub.get().getIsActive()).isTrue();
        
        // 전체 구독 레코드 수 확인 (증가하지 않음)
        long totalSubs = userSubscriptionRepository.countSubscriptionsByUser(testUser);
        assertThat(totalSubs).isEqualTo(1L);
    }

    @Test
    @DisplayName("PRO 구독 취소 시 FREE로 다운그레이드")
    void shouldDowngradeProToFreeWhenCancelled() {
        // Given: 사용자가 PRO 구독을 가지고 있음
        UserSubscription proSubscription = UserSubscription.builder()
                .subscriptionId("integration-sub-003")
                .user(testUser)
                .plan(proPlan)
                .planPaymentDate(LocalDateTime.now().minusDays(5))
                .build();
        proSubscription.setIsActive(true);
        proSubscription.setBillingKey("pro-billing-key");
        userSubscriptionRepository.save(proSubscription);

        String originalSubscriptionId = proSubscription.getSubscriptionId();

        // When: PRO 구독 취소 (FREE로 다운그레이드)
        proSubscription.setPlan(freePlan);
        proSubscription.setBillingKey(null); // 빌링키 제거
        proSubscription.setPlanUpdateDate(LocalDateTime.now());
        userSubscriptionRepository.save(proSubscription);

        // Then: 같은 구독 ID로 FREE 플랜이 활성화됨
        Optional<UserSubscription> downgradedSub = userSubscriptionRepository.findActiveSubscriptionByUser(testUser);
        assertThat(downgradedSub).isPresent();
        assertThat(downgradedSub.get().getSubscriptionId()).isEqualTo(originalSubscriptionId); // 같은 ID
        assertThat(downgradedSub.get().getPlan().getPlanType()).isEqualTo(PlanType.FREE);
        assertThat(downgradedSub.get().getBillingKey()).isNull();
        assertThat(downgradedSub.get().getIsActive()).isTrue(); // 여전히 활성 (FREE 서비스 제공)
        
        // 전체 구독 레코드 수 확인 (증가하지 않음)
        long totalSubs = userSubscriptionRepository.countSubscriptionsByUser(testUser);
        assertThat(totalSubs).isEqualTo(1L);
    }

    @Test
    @DisplayName("사용자는 항상 정확히 하나의 활성 구독만 보유")
    void shouldAlwaysHaveExactlyOneActiveSubscription() {
        // Given: 사용자가 FREE 구독을 시작
        UserSubscription subscription = UserSubscription.builder()
                .subscriptionId("integration-sub-004")
                .user(testUser)
                .plan(freePlan)
                .planPaymentDate(LocalDateTime.now())
                .build();
        subscription.setIsActive(true);
        userSubscriptionRepository.save(subscription);

        // When: 여러 번의 플랜 변경 (FREE -> PRO -> FREE)
        
        // 1. FREE -> PRO 업그레이드
        subscription.setPlan(proPlan);
        subscription.setBillingKey("billing-key-1");
        subscription.setPlanUpdateDate(LocalDateTime.now());
        userSubscriptionRepository.save(subscription);

        // 2. PRO -> FREE 다운그레이드
        subscription.setPlan(freePlan);
        subscription.setBillingKey(null);
        subscription.setPlanUpdateDate(LocalDateTime.now());
        userSubscriptionRepository.save(subscription);

        // 3. FREE -> PRO 재업그레이드
        subscription.setPlan(proPlan);
        subscription.setBillingKey("billing-key-2");
        subscription.setPlanUpdateDate(LocalDateTime.now());
        userSubscriptionRepository.save(subscription);

        // Then: 모든 변경 후에도 정확히 하나의 활성 구독만 보유
        long activeCount = userSubscriptionRepository.countActiveSubscriptionsByUser(testUser);
        long totalCount = userSubscriptionRepository.countSubscriptionsByUser(testUser);
        
        assertThat(activeCount).isEqualTo(1L);
        assertThat(totalCount).isEqualTo(1L);
        
        Optional<UserSubscription> finalSub = userSubscriptionRepository.findActiveSubscriptionByUser(testUser);
        assertThat(finalSub).isPresent();
        assertThat(finalSub.get().getPlan().getPlanType()).isEqualTo(PlanType.PRO);
        assertThat(finalSub.get().getBillingKey()).isEqualTo("billing-key-2");
    }

    @Test
    @DisplayName("데이터 무결성: 사용자별 활성 구독 개수 검증")
    void shouldMaintainDataIntegrityWithSingleActiveSubscription() {
        // Given: 여러 사용자가 구독을 가짐
        User user1 = new User("user-1", "auth0|user1", "user1@test.com");
        User user2 = new User("user-2", "auth0|user2", "user2@test.com");
        userRepository.save(user1);
        userRepository.save(user2);

        // 각 사용자마다 하나씩의 구독 생성
        UserSubscription sub1 = createActiveSubscription("sub-1", user1, freePlan);
        UserSubscription sub2 = createActiveSubscription("sub-2", user2, proPlan);
        userSubscriptionRepository.save(sub1);
        userSubscriptionRepository.save(sub2);

        // When: 다중 활성 구독을 가진 사용자 조회
        List<User> usersWithMultipleActive = userSubscriptionRepository.findUsersWithMultipleActiveSubscriptions();

        // Then: 다중 활성 구독을 가진 사용자가 없어야 함
        assertThat(usersWithMultipleActive).isEmpty();

        // 각 사용자의 활성 구독 개수 확인
        assertThat(userSubscriptionRepository.countActiveSubscriptionsByUser(user1)).isEqualTo(1L);
        assertThat(userSubscriptionRepository.countActiveSubscriptionsByUser(user2)).isEqualTo(1L);
    }

    private UserSubscription createActiveSubscription(String subscriptionId, User user, Plan plan) {
        UserSubscription subscription = UserSubscription.builder()
                .subscriptionId(subscriptionId)
                .user(user)
                .plan(plan)
                .planPaymentDate(LocalDateTime.now())
                .build();
        subscription.setIsActive(true);
        return subscription;
    }
}