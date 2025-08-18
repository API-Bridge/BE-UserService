package org.example.Usersvc.service;

import org.example.Usersvc.common.error.ErrorCode;
import org.example.Usersvc.common.exception.BusinessException;
import org.example.Usersvc.domain.SubscriptionPlan;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.repository.SubscriptionPlanRepository;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserSubscriptionService 테스트
 */
@ExtendWith(MockitoExtension.class)
class UserSubscriptionServiceTest {
    
    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;
    
    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;
    
    @InjectMocks
    private UserSubscriptionService userSubscriptionService;
    
    private User testUser;
    private SubscriptionPlan freePlan;
    private SubscriptionPlan proPlan;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .auth0Id("test-auth0-id")
                .userEmail("test@example.com")
                .build();
        testUser.setUserId("1");
        
        freePlan = SubscriptionPlan.builder()
                .planName("FREE")
                .price(java.math.BigDecimal.ZERO)
                .description("무료 기본 플랜")
                .features("{\"maxApiCount\": 5, \"rateLimitPerMinute\": 60, \"rateLimitPerHour\": 1000, \"rateLimitPerDay\": 10000}")
                .build();
        
        proPlan = SubscriptionPlan.builder()
                .planName("PRO")
                .price(java.math.BigDecimal.valueOf(29000))
                .description("프로 플랜")
                .features("{\"maxApiCount\": 100, \"rateLimitPerMinute\": 600, \"rateLimitPerHour\": 10000, \"rateLimitPerDay\": 100000}")
                .build();
    }
    
    @Test
    @DisplayName("활성 구독이 있으면 해당 구독을 반환해야 한다")
    void shouldReturnActiveSubscriptionWhenExists() {
        // Given
        UserSubscription activeSubscription = UserSubscription.builder()
                .subscriptionId("test-sub-1")
                .user(testUser)
                .plan(proPlan)
                .planPaymentDate(LocalDateTime.now().minusDays(1))
                .build();
        activeSubscription.activate();
        
        when(userSubscriptionRepository.findActiveSubscriptionByUser(testUser))
                .thenReturn(Optional.of(activeSubscription));
        
        // When
        UserSubscription result = userSubscriptionService.getActiveSubscription(testUser);
        
        // Then
        assertThat(result).isEqualTo(activeSubscription);
        assertThat(result.getPlan().getPlanName()).isEqualTo("PRO");
    }
    
    @Test
    @DisplayName("활성 구독이 없으면 기본 무료 구독을 생성해야 한다")
    void shouldCreateDefaultSubscriptionWhenNoActiveSubscription() {
        // Given
        when(userSubscriptionRepository.findActiveSubscriptionByUser(testUser))
                .thenReturn(Optional.empty());
        when(subscriptionPlanRepository.findFreePlan())
                .thenReturn(Optional.of(freePlan));
        when(userSubscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        UserSubscription result = userSubscriptionService.getActiveSubscription(testUser);
        
        // Then
        assertThat(result.getPlan().getPlanName()).isEqualTo("FREE");
        assertThat(result.getUser()).isEqualTo(testUser);
        verify(userSubscriptionRepository).save(any(UserSubscription.class));
    }
    
    @Test
    @DisplayName("새 구독 생성 시 기존 활성 구독을 취소해야 한다")
    void shouldCancelExistingSubscriptionWhenCreatingNew() {
        // Given
        UserSubscription existingSubscription = UserSubscription.builder()
                .subscriptionId("test-sub-2")
                .user(testUser)
                .plan(freePlan)
                .planPaymentDate(LocalDateTime.now().minusDays(10))
                .build();
        existingSubscription.activate();
        
        when(subscriptionPlanRepository.findByPlanName("PRO"))
                .thenReturn(Optional.of(proPlan));
        when(userSubscriptionRepository.findActiveSubscriptionByUser(testUser))
                .thenReturn(Optional.of(existingSubscription));
        when(userSubscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        LocalDateTime expiresAt = LocalDateTime.now().plusMonths(1);
        UserSubscription result = userSubscriptionService.createSubscription(testUser, "PRO", expiresAt);
        
        // Then
        assertThat(result.getPlan().getPlanName()).isEqualTo("PRO");
        assertThat(existingSubscription.isExpired()).isTrue();
        verify(userSubscriptionRepository, times(2)).save(any(UserSubscription.class));
    }
    
    @Test
    @DisplayName("존재하지 않는 플랜으로 구독 생성 시 예외가 발생해야 한다")
    void shouldThrowExceptionWhenCreatingSubscriptionWithInvalidPlan() {
        // Given
        when(subscriptionPlanRepository.findByPlanName("INVALID"))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> 
                userSubscriptionService.createSubscription(testUser, "INVALID", LocalDateTime.now().plusMonths(1)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SUBSCRIPTION_PLAN);
    }
    
    @Test
    @DisplayName("구독 갱신이 정상적으로 동작해야 한다")
    void shouldRenewSubscriptionCorrectly() {
        // Given
        UserSubscription activeSubscription = UserSubscription.builder()
                .subscriptionId("test-sub-3")
                .user(testUser)
                .plan(proPlan)
                .planPaymentDate(LocalDateTime.now().minusDays(20))
                .build();
        activeSubscription.activate();
        
        when(userSubscriptionRepository.findActiveSubscriptionByUser(testUser))
                .thenReturn(Optional.of(activeSubscription));
        when(userSubscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        UserSubscription result = userSubscriptionService.renewSubscription(testUser);
        
        // Then
        assertThat(result.isExpired()).isFalse();
        verify(userSubscriptionRepository).save(activeSubscription);
    }
    
    @Test
    @DisplayName("구독 취소 후 무료 플랜으로 변경되어야 한다")
    void shouldCreateFreeSubscriptionAfterCancellation() {
        // Given
        UserSubscription activeSubscription = UserSubscription.builder()
                .subscriptionId("test-sub-4")
                .user(testUser)
                .plan(proPlan)
                .planPaymentDate(LocalDateTime.now().minusDays(10))
                .build();
        activeSubscription.activate();
        
        when(userSubscriptionRepository.findActiveSubscriptionByUser(testUser))
                .thenReturn(Optional.of(activeSubscription));
        when(subscriptionPlanRepository.findFreePlan())
                .thenReturn(Optional.of(freePlan));
        when(userSubscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        userSubscriptionService.cancelSubscription(testUser);
        
        // Then
        assertThat(activeSubscription.isExpired()).isTrue();
        verify(userSubscriptionRepository, times(2)).save(any(UserSubscription.class));
    }
    
    @Test
    @DisplayName("플랜 변경이 정상적으로 동작해야 한다")
    void shouldChangePlanCorrectly() {
        // Given
        UserSubscription freeSubscription = UserSubscription.builder()
                .subscriptionId("test-sub-5")
                .user(testUser)
                .plan(freePlan)
                .planPaymentDate(LocalDateTime.now().minusDays(5))
                .build();
        freeSubscription.activate();
        
        when(subscriptionPlanRepository.findByPlanName("PRO"))
                .thenReturn(Optional.of(proPlan));
        when(userSubscriptionRepository.findActiveSubscriptionByUser(testUser))
                .thenReturn(Optional.of(freeSubscription));
        when(userSubscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        UserSubscription result = userSubscriptionService.changePlan(testUser, "PRO");
        
        // Then
        assertThat(result.getPlan().getPlanName()).isEqualTo("PRO");
        assertThat(freeSubscription.isExpired()).isTrue();
        verify(userSubscriptionRepository, times(2)).save(any(UserSubscription.class));
    }
}