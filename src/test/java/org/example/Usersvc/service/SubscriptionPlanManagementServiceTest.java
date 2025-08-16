package org.example.Usersvc.service;

import org.example.Usersvc.domain.SubscriptionPlan;
import org.example.Usersvc.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("구독 플랜 관리 서비스 테스트")
class SubscriptionPlanManagementServiceTest {

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @InjectMocks
    private SubscriptionPlanManagementService subscriptionPlanManagementService;

    @Test
    @DisplayName("모든 활성 플랜을 조회할 수 있어야 한다")
    void getAllActivePlans() {
        SubscriptionPlan freePlan = SubscriptionPlan.builder()
                .planName("FREE")
                .maxApiCount(5)
                .rateLimitPerMinute(10)
                .rateLimitPerHour(100)
                .rateLimitPerDay(1000)
                .monthlyPrice(0)
                .build();

        SubscriptionPlan proPlan = SubscriptionPlan.builder()
                .planName("PRO")
                .maxApiCount(50)
                .rateLimitPerMinute(100)
                .rateLimitPerHour(1000)
                .rateLimitPerDay(10000)
                .monthlyPrice(2999)
                .build();

        when(subscriptionPlanRepository.findByIsActiveTrueOrderByMonthlyPrice()).thenReturn(Arrays.asList(freePlan, proPlan));

        List<SubscriptionPlan> plans = subscriptionPlanManagementService.getAllActivePlans();

        assertThat(plans).hasSize(2);
        assertThat(plans.get(0).getPlanName()).isEqualTo("FREE");
        assertThat(plans.get(1).getPlanName()).isEqualTo("PRO");
        verify(subscriptionPlanRepository).findByIsActiveTrueOrderByMonthlyPrice();
    }

    @Test
    @DisplayName("플랜 이름으로 플랜을 조회할 수 있어야 한다")
    void getPlanByName() {
        SubscriptionPlan proPlan = SubscriptionPlan.builder()
                .planName("PRO")
                .maxApiCount(50)
                .rateLimitPerMinute(100)
                .rateLimitPerHour(1000)
                .rateLimitPerDay(10000)
                .monthlyPrice(2999)
                .build();

        when(subscriptionPlanRepository.findByPlanNameAndIsActiveTrue("PRO")).thenReturn(Optional.of(proPlan));

        Optional<SubscriptionPlan> plan = subscriptionPlanManagementService.getPlanByName("PRO");

        assertThat(plan).isPresent();
        assertThat(plan.get().getPlanName()).isEqualTo("PRO");
        assertThat(plan.get().getMaxApiCount()).isEqualTo(50);
        verify(subscriptionPlanRepository).findByPlanNameAndIsActiveTrue("PRO");
    }

    @Test
    @DisplayName("존재하지 않는 플랜 조회 시 빈 Optional을 반환해야 한다")
    void getPlanByName_NotFound() {
        when(subscriptionPlanRepository.findByPlanNameAndIsActiveTrue("NONEXISTENT")).thenReturn(Optional.empty());

        Optional<SubscriptionPlan> plan = subscriptionPlanManagementService.getPlanByName("NONEXISTENT");

        assertThat(plan).isEmpty();
        verify(subscriptionPlanRepository).findByPlanNameAndIsActiveTrue("NONEXISTENT");
    }

    @Test
    @DisplayName("무료 플랜을 조회할 수 있어야 한다")
    void getFreePlan() {
        SubscriptionPlan freePlan = SubscriptionPlan.builder()
                .planName("FREE")
                .maxApiCount(5)
                .rateLimitPerMinute(10)
                .rateLimitPerHour(100)
                .rateLimitPerDay(1000)
                .monthlyPrice(0)
                .build();

        when(subscriptionPlanRepository.findByPlanNameAndIsActiveTrue("FREE")).thenReturn(Optional.of(freePlan));

        SubscriptionPlan plan = subscriptionPlanManagementService.getFreePlan();

        assertThat(plan.getPlanName()).isEqualTo("FREE");
        assertThat(plan.getMonthlyPrice()).isEqualTo(0);
        verify(subscriptionPlanRepository).findByPlanNameAndIsActiveTrue("FREE");
    }

    @Test
    @DisplayName("무료 플랜이 없으면 예외가 발생해야 한다")
    void getFreePlan_NotFound() {
        when(subscriptionPlanRepository.findByPlanNameAndIsActiveTrue("FREE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionPlanManagementService.getFreePlan())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("FREE 플랜을 찾을 수 없습니다.");

        verify(subscriptionPlanRepository).findByPlanNameAndIsActiveTrue("FREE");
    }

    @Test
    @DisplayName("사용자가 플랜의 API 생성 제한을 초과했는지 확인할 수 있어야 한다")
    void isApiCreationLimitExceeded() {
        SubscriptionPlan freePlan = SubscriptionPlan.builder()
                .planName("FREE")
                .maxApiCount(5)
                .rateLimitPerMinute(10)
                .rateLimitPerHour(100)
                .rateLimitPerDay(1000)
                .monthlyPrice(0)
                .build();

        boolean exceededWithin = subscriptionPlanManagementService.isApiCreationLimitExceeded(freePlan, 3);
        boolean exceededEqual = subscriptionPlanManagementService.isApiCreationLimitExceeded(freePlan, 5);
        boolean exceededOver = subscriptionPlanManagementService.isApiCreationLimitExceeded(freePlan, 6);

        assertThat(exceededWithin).isFalse();
        assertThat(exceededEqual).isFalse();
        assertThat(exceededOver).isTrue();
    }

    @Test
    @DisplayName("플랜의 Rate Limit 정보를 가져올 수 있어야 한다")
    void getRateLimitInfo() {
        SubscriptionPlan proPlan = SubscriptionPlan.builder()
                .planName("PRO")
                .maxApiCount(50)
                .rateLimitPerMinute(100)
                .rateLimitPerHour(1000)
                .rateLimitPerDay(10000)
                .monthlyPrice(2999)
                .build();

        SubscriptionPlanManagementService.RateLimitInfo rateLimitInfo = 
                subscriptionPlanManagementService.getRateLimitInfo(proPlan);

        assertThat(rateLimitInfo.getPerMinute()).isEqualTo(100);
        assertThat(rateLimitInfo.getPerHour()).isEqualTo(1000);
        assertThat(rateLimitInfo.getPerDay()).isEqualTo(10000);
    }

    @Test
    @DisplayName("플랜 비교를 통해 업그레이드/다운그레이드 여부를 확인할 수 있어야 한다")
    void comparePlans() {
        SubscriptionPlan freePlan = SubscriptionPlan.builder()
                .planName("FREE")
                .maxApiCount(5)
                .rateLimitPerMinute(10)
                .rateLimitPerHour(100)
                .rateLimitPerDay(1000)
                .monthlyPrice(0)
                .build();

        SubscriptionPlan proPlan = SubscriptionPlan.builder()
                .planName("PRO")
                .maxApiCount(50)
                .rateLimitPerMinute(100)
                .rateLimitPerHour(1000)
                .rateLimitPerDay(10000)
                .monthlyPrice(2999)
                .build();

        boolean freeToProIsUpgrade = subscriptionPlanManagementService.isUpgrade(freePlan, proPlan);
        boolean proToFreeIsUpgrade = subscriptionPlanManagementService.isUpgrade(proPlan, freePlan);

        assertThat(freeToProIsUpgrade).isTrue();
        assertThat(proToFreeIsUpgrade).isFalse();
    }

    @Test
    @DisplayName("플랜별 기능 제한 정보를 조회할 수 있어야 한다")
    void getPlanFeatures() {
        SubscriptionPlan proPlan = SubscriptionPlan.builder()
                .planName("PRO")
                .maxApiCount(50)
                .rateLimitPerMinute(100)
                .rateLimitPerHour(1000)
                .rateLimitPerDay(10000)
                .monthlyPrice(2999)
                .build();

        SubscriptionPlanManagementService.PlanFeatures features = 
                subscriptionPlanManagementService.getPlanFeatures(proPlan);

        assertThat(features.getMaxApiCount()).isEqualTo(50);
        assertThat(features.getMaxDataPerApi()).isEqualTo(20); // PRO 플랜의 기본값
        assertThat(features.isApiSharingAllowed()).isTrue();
        assertThat(features.isPrioritySupport()).isTrue();
    }
}