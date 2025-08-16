package org.example.Usersvc.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Stripe 설정 테스트")
class StripeConfigTest {

    @Autowired
    private StripeProperties stripeProperties;

    @Test
    @DisplayName("Stripe 설정 프로퍼티가 올바르게 로드되어야 한다")
    void stripePropertiesLoaded() {
        assertThat(stripeProperties).isNotNull();
        assertThat(stripeProperties.getSecretKey()).isNotNull();
        assertThat(stripeProperties.getPublicKey()).isNotNull();
        assertThat(stripeProperties.getWebhookSecret()).isNotNull();
    }

    @Test
    @DisplayName("Stripe 플랜 Product ID가 올바르게 설정되어야 한다")
    void stripePlanProductIdsConfigured() {
        assertThat(stripeProperties.getProducts()).isNotNull();
        assertThat(stripeProperties.getProducts().getFree()).isNotNull();
        assertThat(stripeProperties.getProducts().getPro()).isNotNull();
        assertThat(stripeProperties.getProducts().getEnterprise()).isNotNull();
    }

    @Test
    @DisplayName("Stripe 가격 Price ID가 올바르게 설정되어야 한다")
    void stripePriceIdsConfigured() {
        assertThat(stripeProperties.getPrices()).isNotNull();
        assertThat(stripeProperties.getPrices().getProMonthly()).isNotNull();
        assertThat(stripeProperties.getPrices().getProYearly()).isNotNull();
        assertThat(stripeProperties.getPrices().getEnterpriseMonthly()).isNotNull();
        assertThat(stripeProperties.getPrices().getEnterpriseYearly()).isNotNull();
    }

    @Test
    @DisplayName("가격 ID로 플랜 타입을 올바르게 결정해야 한다")
    void getPlanTypeByPriceId() {
        String proMonthlyPriceId = stripeProperties.getPrices().getProMonthly();
        String proYearlyPriceId = stripeProperties.getPrices().getProYearly();
        String enterpriseMonthlyPriceId = stripeProperties.getPrices().getEnterpriseMonthly();
        String enterpriseYearlyPriceId = stripeProperties.getPrices().getEnterpriseYearly();

        assertThat(stripeProperties.getPlanTypeByPriceId(proMonthlyPriceId)).isEqualTo("PRO");
        assertThat(stripeProperties.getPlanTypeByPriceId(proYearlyPriceId)).isEqualTo("PRO");
        assertThat(stripeProperties.getPlanTypeByPriceId(enterpriseMonthlyPriceId)).isEqualTo("ENTERPRISE");
        assertThat(stripeProperties.getPlanTypeByPriceId(enterpriseYearlyPriceId)).isEqualTo("ENTERPRISE");
        assertThat(stripeProperties.getPlanTypeByPriceId("unknown-price-id")).isEqualTo("FREE");
    }

    @Test
    @DisplayName("가격 ID로 청구 주기를 올바르게 결정해야 한다")
    void getBillingPeriodByPriceId() {
        String proMonthlyPriceId = stripeProperties.getPrices().getProMonthly();
        String proYearlyPriceId = stripeProperties.getPrices().getProYearly();
        String enterpriseMonthlyPriceId = stripeProperties.getPrices().getEnterpriseMonthly();
        String enterpriseYearlyPriceId = stripeProperties.getPrices().getEnterpriseYearly();

        assertThat(stripeProperties.getBillingPeriodByPriceId(proMonthlyPriceId)).isEqualTo("MONTHLY");
        assertThat(stripeProperties.getBillingPeriodByPriceId(proYearlyPriceId)).isEqualTo("YEARLY");
        assertThat(stripeProperties.getBillingPeriodByPriceId(enterpriseMonthlyPriceId)).isEqualTo("MONTHLY");
        assertThat(stripeProperties.getBillingPeriodByPriceId(enterpriseYearlyPriceId)).isEqualTo("YEARLY");
        assertThat(stripeProperties.getBillingPeriodByPriceId("unknown-price-id")).isEqualTo("MONTHLY");
    }
}