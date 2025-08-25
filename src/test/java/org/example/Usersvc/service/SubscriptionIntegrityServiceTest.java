package org.example.Usersvc.service;

import org.example.Usersvc.domain.Plan;
import org.example.Usersvc.domain.planName;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionIntegrityServiceTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @InjectMocks
    private SubscriptionIntegrityService subscriptionIntegrityService;

    private User testUser;
    private Plan freePlan;
    private Plan proPlan;
    private UserSubscription activeSubscription1;
    private UserSubscription activeSubscription2;

    @BeforeEach
    void setUp() {
        testUser = new User("test-user-001", "auth0|testuser", "test@example.com");
        
        freePlan = Plan.builder()
                .planName(planName.FREE)
                .price(BigDecimal.ZERO)
                .description("Free Plan")
                .build();
                
        proPlan = Plan.builder()
                .planName(planName.PRO)
                .price(BigDecimal.valueOf(22.00))
                .description("Pro Plan")
                .build();

        activeSubscription1 = UserSubscription.builder()
                .subscriptionId("sub-001")
                .user(testUser)
                .plan(freePlan)
                .planPaymentDate(LocalDateTime.now().minusDays(10))
                .build();

        activeSubscription2 = UserSubscription.builder()
                .subscriptionId("sub-002")
                .user(testUser)
                .plan(proPlan)
                .planPaymentDate(LocalDateTime.now().minusDays(5))
                .build();
    }

    @Test
    @DisplayName("다중 활성 구독이 있는 경우 무결성 검증 실패")
    void shouldDetectMultipleActiveSubscriptions() {
        // Given
        List<User> usersWithMultipleActive = Arrays.asList(testUser);
        when(userSubscriptionRepository.findUsersWithMultipleActiveSubscriptions())
                .thenReturn(usersWithMultipleActive);
        when(userSubscriptionRepository.getSubscriptionStatistics())
                .thenReturn(new Object[]{1L, 2L, 2L, 0L}); // 1 user, 2 total subs, 2 active, 0 inactive
        when(userSubscriptionRepository.findAllSubscriptionsByUserOrderByLatest(testUser))
                .thenReturn(Arrays.asList(activeSubscription2, activeSubscription1)); // PRO가 더 최신
        when(userSubscriptionRepository.countActiveSubscriptionsByUser(testUser))
                .thenReturn(2L);
        when(userSubscriptionRepository.countSubscriptionsByUser(testUser))
                .thenReturn(2L);

        // When
        SubscriptionIntegrityService.IntegrityReport report = subscriptionIntegrityService.performFullIntegrityCheck();

        // Then
        assertThat(report.hasIntegrityIssues()).isTrue();
        assertThat(report.getUsersWithMultipleActiveSubscriptions()).hasSize(1);
        assertThat(report.getTotalUsers()).isEqualTo(1L);
        assertThat(report.getActiveSubscriptions()).isEqualTo(2L);
        assertThat(report.getUserIssueDetails()).containsKey("test-user-001");
        
        SubscriptionIntegrityService.UserIntegrityStatus userStatus = 
                report.getUserIssueDetails().get("test-user-001");
        assertThat(userStatus.getActiveSubscriptionCount()).isEqualTo(2L);
        assertThat(userStatus.isHasIntegrityIssue()).isTrue();
    }

    @Test
    @DisplayName("정상적인 구독 상태에서는 무결성 검증 통과")
    void shouldPassIntegrityCheckWithNormalSubscriptions() {
        // Given
        when(userSubscriptionRepository.findUsersWithMultipleActiveSubscriptions())
                .thenReturn(Collections.emptyList());
        when(userSubscriptionRepository.getSubscriptionStatistics())
                .thenReturn(new Object[]{1L, 1L, 1L, 0L}); // 1 user, 1 total sub, 1 active, 0 inactive

        // When
        SubscriptionIntegrityService.IntegrityReport report = subscriptionIntegrityService.performFullIntegrityCheck();

        // Then
        assertThat(report.hasIntegrityIssues()).isFalse();
        assertThat(report.getUsersWithMultipleActiveSubscriptions()).isEmpty();
        assertThat(report.getTotalUsers()).isEqualTo(1L);
        assertThat(report.getActiveSubscriptions()).isEqualTo(1L);
    }

    @Test
    @DisplayName("사용자별 구독 무결성 검증 - 정상 사용자")
    void shouldCheckUserIntegrityForNormalUser() {
        // Given
        when(userSubscriptionRepository.countActiveSubscriptionsByUser(testUser)).thenReturn(1L);
        when(userSubscriptionRepository.countSubscriptionsByUser(testUser)).thenReturn(1L);
        when(userSubscriptionRepository.findAllSubscriptionsByUserOrderByLatest(testUser))
                .thenReturn(Arrays.asList(activeSubscription1));

        // When
        SubscriptionIntegrityService.UserIntegrityStatus status = 
                subscriptionIntegrityService.checkUserIntegrity(testUser);

        // Then
        assertThat(status.isHasIntegrityIssue()).isFalse();
        assertThat(status.getActiveSubscriptionCount()).isEqualTo(1L);
        assertThat(status.getTotalSubscriptionCount()).isEqualTo(1L);
        assertThat(status.getUserId()).isEqualTo("test-user-001");
    }

    @Test
    @DisplayName("사용자별 구독 무결성 검증 - 문제 사용자")
    void shouldCheckUserIntegrityForProblemUser() {
        // Given
        when(userSubscriptionRepository.countActiveSubscriptionsByUser(testUser)).thenReturn(2L);
        when(userSubscriptionRepository.countSubscriptionsByUser(testUser)).thenReturn(2L);
        when(userSubscriptionRepository.findAllSubscriptionsByUserOrderByLatest(testUser))
                .thenReturn(Arrays.asList(activeSubscription2, activeSubscription1));

        // When
        SubscriptionIntegrityService.UserIntegrityStatus status = 
                subscriptionIntegrityService.checkUserIntegrity(testUser);

        // Then
        assertThat(status.isHasIntegrityIssue()).isTrue();
        assertThat(status.getActiveSubscriptionCount()).isEqualTo(2L);
        assertThat(status.getTotalSubscriptionCount()).isEqualTo(2L);
        assertThat(status.getAllSubscriptions()).hasSize(2);
    }

    @Test
    @DisplayName("다중 활성 구독 자동 복구 - DRY RUN")
    void shouldRepairMultipleActiveSubscriptionsDryRun() {
        // Given
        List<User> problemUsers = Arrays.asList(testUser);
        when(userSubscriptionRepository.findUsersWithMultipleActiveSubscriptions())
                .thenReturn(problemUsers);
        when(userSubscriptionRepository.findAllSubscriptionsByUserOrderByLatest(testUser))
                .thenReturn(Arrays.asList(activeSubscription2, activeSubscription1)); // PRO가 더 최신

        // When
        int repairedCount = subscriptionIntegrityService.repairMultipleActiveSubscriptions(true);

        // Then
        assertThat(repairedCount).isEqualTo(1); // activeSubscription1만 비활성화될 예정
        verify(userSubscriptionRepository, never()).save(any()); // DRY RUN이므로 저장 안 함
    }

    @Test
    @DisplayName("다중 활성 구독 자동 복구 - 실제 실행")
    void shouldRepairMultipleActiveSubscriptionsActual() {
        // Given
        List<User> problemUsers = Arrays.asList(testUser);
        when(userSubscriptionRepository.findUsersWithMultipleActiveSubscriptions())
                .thenReturn(problemUsers);
        when(userSubscriptionRepository.findAllSubscriptionsByUserOrderByLatest(testUser))
                .thenReturn(Arrays.asList(activeSubscription2, activeSubscription1)); // PRO가 더 최신

        // When
        int repairedCount = subscriptionIntegrityService.repairMultipleActiveSubscriptions(false);

        // Then
        assertThat(repairedCount).isEqualTo(1);
        verify(userSubscriptionRepository, times(1)).save(activeSubscription1);
        assertThat(activeSubscription1.isActive()).isFalse(); // 첫 번째가 아니므로 비활성화
        assertThat(activeSubscription2.isActive()).isTrue();  // 첫 번째(최신)는 활성 유지
    }

    @Test
    @DisplayName("문제가 없는 경우 자동 복구에서 아무 작업 안 함")
    void shouldNotRepairWhenNoIssues() {
        // Given
        when(userSubscriptionRepository.findUsersWithMultipleActiveSubscriptions())
                .thenReturn(Collections.emptyList());

        // When
        int repairedCount = subscriptionIntegrityService.repairMultipleActiveSubscriptions(false);

        // Then
        assertThat(repairedCount).isEqualTo(0);
        verify(userSubscriptionRepository, never()).save(any());
    }
}