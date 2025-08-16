package org.example.Usersvc.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Stripe 프로퍼티 테스트")
class StripePropertiesTest {

    private StripeProperties stripeProperties;

    @BeforeEach
    void setUp() {
        stripeProperties = new StripeProperties();
        stripeProperties.setSecretKey("sk_test_fake_key");
        stripeProperties.setPublicKey("pk_test_fake_key");
        stripeProperties.setWebhookSecret("whsec_fake_secret");

        StripeProperties.Products products = new StripeProperties.Products();
        products.setFree("prod_fake_free");
        products.setPro("prod_fake_pro");
        products.setEnterprise("prod_fake_enterprise");
        stripeProperties.setProducts(products);

        StripeProperties.Prices prices = new StripeProperties.Prices();
        prices.setProMonthly("price_fake_pro_monthly");
        prices.setProYearly("price_fake_pro_yearly");
        prices.setEnterpriseMonthly("price_fake_enterprise_monthly");
        prices.setEnterpriseYearly("price_fake_enterprise_yearly");
        stripeProperties.setPrices(prices);
    }

    @Test
    @DisplayName("가격 ID로 플랜 타입을 올바르게 결정해야 한다")
    void getPlanTypeByPriceId() {
        assertThat(stripeProperties.getPlanTypeByPriceId("price_fake_pro_monthly")).isEqualTo("PRO");
        assertThat(stripeProperties.getPlanTypeByPriceId("price_fake_pro_yearly")).isEqualTo("PRO");
        assertThat(stripeProperties.getPlanTypeByPriceId("price_fake_enterprise_monthly")).isEqualTo("ENTERPRISE");
        assertThat(stripeProperties.getPlanTypeByPriceId("price_fake_enterprise_yearly")).isEqualTo("ENTERPRISE");
        assertThat(stripeProperties.getPlanTypeByPriceId("unknown-price-id")).isEqualTo("FREE");
        assertThat(stripeProperties.getPlanTypeByPriceId(null)).isEqualTo("FREE");
    }

    @Test
    @DisplayName("가격 ID로 청구 주기를 올바르게 결정해야 한다")
    void getBillingPeriodByPriceId() {
        assertThat(stripeProperties.getBillingPeriodByPriceId("price_fake_pro_monthly")).isEqualTo("MONTHLY");
        assertThat(stripeProperties.getBillingPeriodByPriceId("price_fake_pro_yearly")).isEqualTo("YEARLY");
        assertThat(stripeProperties.getBillingPeriodByPriceId("price_fake_enterprise_monthly")).isEqualTo("MONTHLY");
        assertThat(stripeProperties.getBillingPeriodByPriceId("price_fake_enterprise_yearly")).isEqualTo("YEARLY");
        assertThat(stripeProperties.getBillingPeriodByPriceId("unknown-price-id")).isEqualTo("MONTHLY");
        assertThat(stripeProperties.getBillingPeriodByPriceId(null)).isEqualTo("MONTHLY");
    }

    @Test
    @DisplayName("플랜 타입으로 Product ID를 올바르게 반환해야 한다")
    void getProductIdByPlanType() {
        assertThat(stripeProperties.getProductIdByPlanType("FREE")).isEqualTo("prod_fake_free");
        assertThat(stripeProperties.getProductIdByPlanType("PRO")).isEqualTo("prod_fake_pro");
        assertThat(stripeProperties.getProductIdByPlanType("ENTERPRISE")).isEqualTo("prod_fake_enterprise");
        assertThat(stripeProperties.getProductIdByPlanType("unknown")).isEqualTo("prod_fake_free");
    }

    @Test
    @DisplayName("플랜 타입으로 월간 Price ID를 올바르게 반환해야 한다")
    void getMonthlyPriceIdByPlanType() {
        assertThat(stripeProperties.getMonthlyPriceIdByPlanType("PRO")).isEqualTo("price_fake_pro_monthly");
        assertThat(stripeProperties.getMonthlyPriceIdByPlanType("ENTERPRISE")).isEqualTo("price_fake_enterprise_monthly");
        assertThat(stripeProperties.getMonthlyPriceIdByPlanType("FREE")).isNull();
        assertThat(stripeProperties.getMonthlyPriceIdByPlanType("unknown")).isNull();
    }

    @Test
    @DisplayName("플랜 타입으로 연간 Price ID를 올바르게 반환해야 한다")
    void getYearlyPriceIdByPlanType() {
        assertThat(stripeProperties.getYearlyPriceIdByPlanType("PRO")).isEqualTo("price_fake_pro_yearly");
        assertThat(stripeProperties.getYearlyPriceIdByPlanType("ENTERPRISE")).isEqualTo("price_fake_enterprise_yearly");
        assertThat(stripeProperties.getYearlyPriceIdByPlanType("FREE")).isNull();
        assertThat(stripeProperties.getYearlyPriceIdByPlanType("unknown")).isNull();
    }
}