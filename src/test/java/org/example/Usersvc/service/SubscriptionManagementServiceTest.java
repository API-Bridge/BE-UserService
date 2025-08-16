package org.example.Usersvc.service;

import org.example.Usersvc.config.StripeProperties;
import org.example.Usersvc.domain.SubscriptionPlan;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("구독 관리 서비스 테스트")
class SubscriptionManagementServiceTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private SubscriptionPlanManagementService planManagementService;

    @Mock
    private StripeSubscriptionService stripeSubscriptionService;

    @Mock
    private StripeCustomerService stripeCustomerService;

    @Mock
    private StripeProperties stripeProperties;

    @InjectMocks
    private SubscriptionManagementService subscriptionManagementService;

    @Test
    @DisplayName("새로운 유료 구독을 생성할 수 있어야 한다")
    void createPaidSubscription() {
        User user = User.builder()
                .userId("user-12345")
                .auth0Id("auth0|12345")
                .userEmail("test@example.com")
                .build();

        SubscriptionPlan proPlan = SubscriptionPlan.builder()
                .planName("PRO")
                .maxApiCount(50)
                .rateLimitPerMinute(100)
                .rateLimitPerHour(1000)
                .rateLimitPerDay(10000)
                .monthlyPrice(2999)
                .build();

        String customerId = "cus_stripe_customer_id";
        String subscriptionId = "sub_stripe_subscription_id";
        String priceId = "price_pro_monthly";

        when(planManagementService.getPlanByName("PRO")).thenReturn(java.util.Optional.of(proPlan));
        when(stripeProperties.getMonthlyPriceIdByPlanType("PRO")).thenReturn(priceId);
        when(stripeCustomerService.findCustomerByEmail(user.getUserEmail())).thenReturn(null);
        when(stripeCustomerService.createCustomer(user)).thenReturn(customerId);
        when(stripeSubscriptionService.createSubscription(customerId, priceId)).thenReturn(subscriptionId);

        UserSubscription savedSubscription = UserSubscription.builder()
                .user(user)
                .plan(proPlan)
                .startedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMonths(1))
                .build();

        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenReturn(savedSubscription);

        UserSubscription result = subscriptionManagementService.createSubscription(user, "PRO", "MONTHLY");

        assertThat(result).isNotNull();
        assertThat(result.getPlan().getPlanName()).isEqualTo("PRO");
        assertThat(result.getUser()).isEqualTo(user);

        verify(stripeCustomerService).createCustomer(user);
        verify(stripeSubscriptionService).createSubscription(customerId, priceId);
        verify(userSubscriptionRepository).save(any(UserSubscription.class));
    }

    @Test
    @DisplayName("기존 고객의 구독을 생성할 수 있어야 한다")
    void createSubscriptionForExistingCustomer() {
        User user = User.builder()
                .userId("user-12345")
                .auth0Id("auth0|12345")
                .userEmail("test@example.com")
                .build();

        SubscriptionPlan proPlan = SubscriptionPlan.builder()
                .planName("PRO")
                .maxApiCount(50)
                .rateLimitPerMinute(100)
                .rateLimitPerHour(1000)
                .rateLimitPerDay(10000)
                .monthlyPrice(2999)
                .build();

        String existingCustomerId = "cus_existing_customer_id";
        String subscriptionId = "sub_stripe_subscription_id";
        String priceId = "price_pro_monthly";

        when(planManagementService.getPlanByName("PRO")).thenReturn(java.util.Optional.of(proPlan));
        when(stripeProperties.getMonthlyPriceIdByPlanType("PRO")).thenReturn(priceId);
        when(stripeCustomerService.findCustomerByEmail(user.getUserEmail())).thenReturn(existingCustomerId);
        when(stripeSubscriptionService.createSubscription(existingCustomerId, priceId)).thenReturn(subscriptionId);

        UserSubscription savedSubscription = UserSubscription.builder()
                .user(user)
                .plan(proPlan)
                .startedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMonths(1))
                .build();

        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenReturn(savedSubscription);

        UserSubscription result = subscriptionManagementService.createSubscription(user, "PRO", "MONTHLY");

        assertThat(result).isNotNull();
        verify(stripeCustomerService, never()).createCustomer(any());
        verify(stripeSubscriptionService).createSubscription(existingCustomerId, priceId);
    }

    @Test
    @DisplayName("무료 플랜 구독을 생성할 수 있어야 한다")
    void createFreeSubscription() {
        User user = User.builder()
                .userId("user-12345")
                .auth0Id("auth0|12345")
                .userEmail("test@example.com")
                .build();

        SubscriptionPlan freePlan = SubscriptionPlan.builder()
                .planName("FREE")
                .maxApiCount(5)
                .rateLimitPerMinute(10)
                .rateLimitPerHour(100)
                .rateLimitPerDay(1000)
                .monthlyPrice(0)
                .build();

        when(planManagementService.getPlanByName("FREE")).thenReturn(java.util.Optional.of(freePlan));

        UserSubscription savedSubscription = UserSubscription.builder()
                .user(user)
                .plan(freePlan)
                .startedAt(LocalDateTime.now())
                .expiresAt(null)
                .build();

        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenReturn(savedSubscription);

        UserSubscription result = subscriptionManagementService.createSubscription(user, "FREE", "MONTHLY");

        assertThat(result).isNotNull();
        assertThat(result.getPlan().getPlanName()).isEqualTo("FREE");
        assertThat(result.getExpiresAt()).isNull();

        verify(stripeCustomerService, never()).createCustomer(any());
        verify(stripeSubscriptionService, never()).createSubscription(any(), any());
        verify(userSubscriptionRepository).save(any(UserSubscription.class));
    }

    @Test
    @DisplayName("구독을 취소할 수 있어야 한다")
    void cancelSubscription() {
        User user = User.builder()
                .userId("user-12345")
                .auth0Id("auth0|12345")
                .userEmail("test@example.com")
                .build();

        SubscriptionPlan proPlan = SubscriptionPlan.builder()
                .planName("PRO")
                .maxApiCount(50)
                .rateLimitPerMinute(100)
                .rateLimitPerHour(1000)
                .rateLimitPerDay(10000)
                .monthlyPrice(2999)
                .build();

        UserSubscription activeSubscription = UserSubscription.builder()
                .user(user)
                .plan(proPlan)
                .startedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMonths(1))
                .build();

        String stripeSubscriptionId = "sub_stripe_subscription_id";

        when(userSubscriptionRepository.findActiveSubscriptionByUser(user))
                .thenReturn(java.util.Optional.of(activeSubscription));
        when(stripeSubscriptionService.cancelSubscription(stripeSubscriptionId)).thenReturn(true);

        subscriptionManagementService.cancelSubscription(user, stripeSubscriptionId);

        assertThat(activeSubscription.getIsActive()).isFalse();
        verify(stripeSubscriptionService).cancelSubscription(stripeSubscriptionId);
        verify(userSubscriptionRepository).save(activeSubscription);
    }

    @Test
    @DisplayName("구독을 변경할 수 있어야 한다")
    void changeSubscription() {
        User user = User.builder()
                .userId("user-12345")
                .auth0Id("auth0|12345")
                .userEmail("test@example.com")
                .build();

        SubscriptionPlan currentPlan = SubscriptionPlan.builder()
                .planName("PRO")
                .maxApiCount(50)
                .rateLimitPerMinute(100)
                .rateLimitPerHour(1000)
                .rateLimitPerDay(10000)
                .monthlyPrice(2999)
                .build();

        SubscriptionPlan newPlan = SubscriptionPlan.builder()
                .planName("ENTERPRISE")
                .maxApiCount(500)
                .rateLimitPerMinute(1000)
                .rateLimitPerHour(10000)
                .rateLimitPerDay(100000)
                .monthlyPrice(9999)
                .build();

        UserSubscription currentSubscription = UserSubscription.builder()
                .user(user)
                .plan(currentPlan)
                .startedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMonths(1))
                .build();

        String stripeSubscriptionId = "sub_current_subscription";
        String newPriceId = "price_enterprise_monthly";

        when(userSubscriptionRepository.findActiveSubscriptionByUser(user))
                .thenReturn(java.util.Optional.of(currentSubscription));
        when(planManagementService.getPlanByName("ENTERPRISE")).thenReturn(java.util.Optional.of(newPlan));
        when(stripeProperties.getMonthlyPriceIdByPlanType("ENTERPRISE")).thenReturn(newPriceId);
        when(stripeSubscriptionService.updateSubscription(stripeSubscriptionId, newPriceId))
                .thenReturn("sub_updated_subscription");

        UserSubscription updatedSubscription = UserSubscription.builder()
                .user(user)
                .plan(newPlan)
                .startedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMonths(1))
                .build();

        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenReturn(updatedSubscription);

        UserSubscription result = subscriptionManagementService.changeSubscription(user, stripeSubscriptionId, "ENTERPRISE", "MONTHLY");

        assertThat(result).isNotNull();
        assertThat(result.getPlan().getPlanName()).isEqualTo("ENTERPRISE");

        verify(stripeSubscriptionService).updateSubscription(stripeSubscriptionId, newPriceId);
        verify(userSubscriptionRepository).save(any(UserSubscription.class));
    }

    @Test
    @DisplayName("존재하지 않는 플랜으로 구독 생성 시 예외가 발생해야 한다")
    void createSubscription_PlanNotFound() {
        User user = User.builder()
                .userId("user-12345")
                .auth0Id("auth0|12345")
                .userEmail("test@example.com")
                .build();

        when(planManagementService.getPlanByName("NONEXISTENT")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> subscriptionManagementService.createSubscription(user, "NONEXISTENT", "MONTHLY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("플랜을 찾을 수 없습니다: NONEXISTENT");

        verify(stripeCustomerService, never()).createCustomer(any());
        verify(stripeSubscriptionService, never()).createSubscription(any(), any());
    }
}