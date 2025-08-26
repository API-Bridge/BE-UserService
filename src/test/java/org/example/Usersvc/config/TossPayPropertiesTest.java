package org.example.Usersvc.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TossPay 프로퍼티 테스트")
class TossPayPropertiesTest {

    private TossPayProperties tossPayProperties;
    private TossPayProperties.Prices prices;

    @BeforeEach
    void setUp() {
        tossPayProperties = new TossPayProperties();
        tossPayProperties.setClientKey("test_client_key");
        tossPayProperties.setSecretKey("test_secret_key");
        tossPayProperties.setSuccessUrl("http://test.com/success");
        tossPayProperties.setFailUrl("http://test.com/fail");
        tossPayProperties.setConfirmUrl("http://test.com/confirm");

        prices = new TossPayProperties.Prices();
        prices.setProMonthly(22000);
        prices.setFreeMonthly(0);
        tossPayProperties.setPrices(prices);
    }

    @Test
    @DisplayName("TossPay 설정값 조회 테스트")
    void shouldGetTossPayProperties() {
        // then
        assertThat(tossPayProperties.getClientKey()).isEqualTo("test_client_key");
        assertThat(tossPayProperties.getSecretKey()).isEqualTo("test_secret_key");
        assertThat(tossPayProperties.getSuccessUrl()).isEqualTo("http://test.com/success");
        assertThat(tossPayProperties.getFailUrl()).isEqualTo("http://test.com/fail");
        assertThat(tossPayProperties.getConfirmUrl()).isEqualTo("http://test.com/confirm");
    }

    @Test
    @DisplayName("TossPay 가격 설정값 조회 테스트")
    void shouldGetTossPayPrices() {
        // then
        assertThat(tossPayProperties.getPrices()).isNotNull();
        assertThat(tossPayProperties.getPrices().getProMonthly()).isEqualTo(22000);
        assertThat(tossPayProperties.getPrices().getFreeMonthly()).isEqualTo(0);
    }

    @Test
    @DisplayName("TossPay 프로퍼티 설정 테스트")
    void shouldSetTossPayProperties() {
        // given
        TossPayProperties newProperties = new TossPayProperties();

        // when
        newProperties.setClientKey("new_client_key");
        newProperties.setSecretKey("new_secret_key");
        newProperties.setSuccessUrl("http://new.com/success");
        newProperties.setFailUrl("http://new.com/fail");
        newProperties.setConfirmUrl("http://new.com/confirm");

        // then
        assertThat(newProperties.getClientKey()).isEqualTo("new_client_key");
        assertThat(newProperties.getSecretKey()).isEqualTo("new_secret_key");
        assertThat(newProperties.getSuccessUrl()).isEqualTo("http://new.com/success");
        assertThat(newProperties.getFailUrl()).isEqualTo("http://new.com/fail");
        assertThat(newProperties.getConfirmUrl()).isEqualTo("http://new.com/confirm");
    }

    @Test
    @DisplayName("TossPay 가격 설정 테스트")
    void shouldSetTossPayPrices() {
        // given
        TossPayProperties.Prices newPrices = new TossPayProperties.Prices();

        // when
        newPrices.setProMonthly(25000);
        newPrices.setFreeMonthly(0);

        // then
        assertThat(newPrices.getProMonthly()).isEqualTo(25000);
        assertThat(newPrices.getFreeMonthly()).isEqualTo(0);
    }
}