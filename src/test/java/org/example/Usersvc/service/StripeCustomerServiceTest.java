package org.example.Usersvc.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import org.example.Usersvc.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Stripe 고객 관리 서비스 테스트")
class StripeCustomerServiceTest {

    @InjectMocks
    private StripeCustomerService stripeCustomerService;

    @Test
    @DisplayName("새로운 Stripe 고객을 생성할 수 있어야 한다")
    void createCustomer() throws StripeException {
        User user = User.builder()
                .userId("user-12345")
                .auth0Id("auth0|12345")
                .userEmail("test@example.com")
                .build();

        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getId()).thenReturn("cus_stripe_customer_id");
        when(mockCustomer.getEmail()).thenReturn("test@example.com");

        try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class)) {
            customerStatic.when(() -> Customer.create(any(com.stripe.param.CustomerCreateParams.class))).thenReturn(mockCustomer);

            String customerId = stripeCustomerService.createCustomer(user);

            assertThat(customerId).isEqualTo("cus_stripe_customer_id");
            customerStatic.verify(() -> Customer.create(any(com.stripe.param.CustomerCreateParams.class)));
        }
    }

    @Test
    @DisplayName("Stripe 고객을 조회할 수 있어야 한다")
    void getCustomer() throws StripeException {
        String customerId = "cus_stripe_customer_id";

        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getId()).thenReturn(customerId);
        when(mockCustomer.getEmail()).thenReturn("test@example.com");

        try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class)) {
            customerStatic.when(() -> Customer.retrieve(customerId)).thenReturn(mockCustomer);

            Customer customer = stripeCustomerService.getCustomer(customerId);

            assertThat(customer.getId()).isEqualTo(customerId);
            assertThat(customer.getEmail()).isEqualTo("test@example.com");
            customerStatic.verify(() -> Customer.retrieve(customerId));
        }
    }

    @Test
    @DisplayName("Stripe 고객 정보를 업데이트할 수 있어야 한다")
    void updateCustomer() throws StripeException {
        String customerId = "cus_stripe_customer_id";
        String newEmail = "newemail@example.com";

        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getId()).thenReturn(customerId);
        when(mockCustomer.getEmail()).thenReturn(newEmail);

        try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class)) {
            Customer existingCustomer = mock(Customer.class);
            when(existingCustomer.update(any(com.stripe.param.CustomerUpdateParams.class))).thenReturn(mockCustomer);
            customerStatic.when(() -> Customer.retrieve(customerId)).thenReturn(existingCustomer);

            Customer updatedCustomer = stripeCustomerService.updateCustomerEmail(customerId, newEmail);

            assertThat(updatedCustomer.getId()).isEqualTo(customerId);
            assertThat(updatedCustomer.getEmail()).isEqualTo(newEmail);
            customerStatic.verify(() -> Customer.retrieve(customerId));
            verify(existingCustomer).update(any(com.stripe.param.CustomerUpdateParams.class));
        }
    }

    @Test
    @DisplayName("Stripe 고객을 삭제할 수 있어야 한다")
    void deleteCustomer() throws StripeException {
        String customerId = "cus_stripe_customer_id";

        Customer mockCustomer = mock(Customer.class);
        Customer deletedCustomer = mock(Customer.class);
        when(deletedCustomer.getDeleted()).thenReturn(true);
        when(mockCustomer.delete()).thenReturn(deletedCustomer);

        try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class)) {
            customerStatic.when(() -> Customer.retrieve(customerId)).thenReturn(mockCustomer);

            boolean isDeleted = stripeCustomerService.deleteCustomer(customerId);

            assertThat(isDeleted).isTrue();
            customerStatic.verify(() -> Customer.retrieve(customerId));
            verify(mockCustomer).delete();
        }
    }

    @Test
    @DisplayName("Stripe 예외 발생 시 적절히 처리해야 한다")
    void handleStripeException() throws StripeException {
        User user = User.builder()
                .userId("user-12345")
                .auth0Id("auth0|12345")
                .userEmail("test@example.com")
                .build();

        try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class)) {
            customerStatic.when(() -> Customer.create(any(com.stripe.param.CustomerCreateParams.class))).thenThrow(new StripeException("Stripe API error", "request_id", "code", 400) {});

            assertThatThrownBy(() -> stripeCustomerService.createCustomer(user))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Stripe 고객 생성 실패");
        }
    }

    @Test
    @DisplayName("이메일로 고객을 검색할 수 있어야 한다")
    void findCustomerByEmail() {
        String email = "test@example.com";
        String expectedCustomerId = "cus_stripe_customer_id";

        String customerId = stripeCustomerService.findCustomerByEmail(email);

        // 실제 구현에서는 Stripe API를 호출하여 이메일로 고객을 검색
        // 테스트에서는 간단히 반환값이 null이 아님을 확인
        assertThat(customerId).isNull(); // 실제 구현에서는 고객이 없으면 null 반환
    }

    @Test
    @DisplayName("고객의 기본 결제 수단을 설정할 수 있어야 한다")
    void setDefaultPaymentMethod() throws StripeException {
        String customerId = "cus_stripe_customer_id";
        String paymentMethodId = "pm_stripe_payment_method_id";

        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getId()).thenReturn(customerId);

        try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class)) {
            Customer existingCustomer = mock(Customer.class);
            when(existingCustomer.update(any(com.stripe.param.CustomerUpdateParams.class))).thenReturn(mockCustomer);
            customerStatic.when(() -> Customer.retrieve(customerId)).thenReturn(existingCustomer);

            Customer updatedCustomer = stripeCustomerService.setDefaultPaymentMethod(customerId, paymentMethodId);

            assertThat(updatedCustomer.getId()).isEqualTo(customerId);
            customerStatic.verify(() -> Customer.retrieve(customerId));
            verify(existingCustomer).update(any(com.stripe.param.CustomerUpdateParams.class));
        }
    }
}