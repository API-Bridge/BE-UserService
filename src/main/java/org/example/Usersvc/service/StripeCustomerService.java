package org.example.Usersvc.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerCollection;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListParams;
import com.stripe.param.CustomerUpdateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeCustomerService {

    public String createCustomer(User user) {
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(user.getUserEmail())
                    .setName(user.getUserId())
                    .putMetadata("user_id", user.getUserId())
                    .putMetadata("auth0_id", user.getAuth0Id())
                    .build();

            Customer customer = Customer.create(params);
            log.info("Stripe 고객 생성 완료 - customerId: {}, userId: {}", customer.getId(), user.getUserId());
            return customer.getId();
        } catch (StripeException e) {
            log.error("Stripe 고객 생성 실패 - userId: {}, error: {}", user.getUserId(), e.getMessage());
            throw new RuntimeException("Stripe 고객 생성 실패: " + e.getMessage(), e);
        }
    }

    public Customer getCustomer(String customerId) {
        try {
            Customer customer = Customer.retrieve(customerId);
            log.debug("Stripe 고객 조회 완료 - customerId: {}", customerId);
            return customer;
        } catch (StripeException e) {
            log.error("Stripe 고객 조회 실패 - customerId: {}, error: {}", customerId, e.getMessage());
            throw new RuntimeException("Stripe 고객 조회 실패: " + e.getMessage(), e);
        }
    }

    public Customer updateCustomerEmail(String customerId, String newEmail) {
        try {
            Customer customer = Customer.retrieve(customerId);
            
            CustomerUpdateParams params = CustomerUpdateParams.builder()
                    .setEmail(newEmail)
                    .build();

            Customer updatedCustomer = customer.update(params);
            log.info("Stripe 고객 이메일 업데이트 완료 - customerId: {}, newEmail: {}", customerId, newEmail);
            return updatedCustomer;
        } catch (StripeException e) {
            log.error("Stripe 고객 이메일 업데이트 실패 - customerId: {}, error: {}", customerId, e.getMessage());
            throw new RuntimeException("Stripe 고객 이메일 업데이트 실패: " + e.getMessage(), e);
        }
    }

    public boolean deleteCustomer(String customerId) {
        try {
            Customer customer = Customer.retrieve(customerId);
            Customer deletedCustomer = customer.delete();
            
            boolean isDeleted = deletedCustomer.getDeleted() != null && deletedCustomer.getDeleted();
            log.info("Stripe 고객 삭제 완료 - customerId: {}, deleted: {}", customerId, isDeleted);
            return isDeleted;
        } catch (StripeException e) {
            log.error("Stripe 고객 삭제 실패 - customerId: {}, error: {}", customerId, e.getMessage());
            throw new RuntimeException("Stripe 고객 삭제 실패: " + e.getMessage(), e);
        }
    }

    public String findCustomerByEmail(String email) {
        try {
            CustomerListParams params = CustomerListParams.builder()
                    .setEmail(email)
                    .setLimit(1L)
                    .build();

            CustomerCollection customers = Customer.list(params);
            
            if (customers.getData().isEmpty()) {
                log.debug("이메일로 Stripe 고객을 찾을 수 없음 - email: {}", email);
                return null;
            }

            String customerId = customers.getData().get(0).getId();
            log.debug("이메일로 Stripe 고객 찾기 완료 - email: {}, customerId: {}", email, customerId);
            return customerId;
        } catch (StripeException e) {
            log.error("이메일로 Stripe 고객 검색 실패 - email: {}, error: {}", email, e.getMessage());
            throw new RuntimeException("Stripe 고객 검색 실패: " + e.getMessage(), e);
        }
    }

    public Customer setDefaultPaymentMethod(String customerId, String paymentMethodId) {
        try {
            Customer customer = Customer.retrieve(customerId);
            
            CustomerUpdateParams params = CustomerUpdateParams.builder()
                    .setInvoiceSettings(
                            CustomerUpdateParams.InvoiceSettings.builder()
                                    .setDefaultPaymentMethod(paymentMethodId)
                                    .build()
                    )
                    .build();

            Customer updatedCustomer = customer.update(params);
            log.info("Stripe 고객 기본 결제 수단 설정 완료 - customerId: {}, paymentMethodId: {}", 
                    customerId, paymentMethodId);
            return updatedCustomer;
        } catch (StripeException e) {
            log.error("Stripe 고객 기본 결제 수단 설정 실패 - customerId: {}, error: {}", customerId, e.getMessage());
            throw new RuntimeException("Stripe 고객 기본 결제 수단 설정 실패: " + e.getMessage(), e);
        }
    }

    public Customer updateCustomerMetadata(String customerId, Map<String, String> metadata) {
        try {
            Customer customer = Customer.retrieve(customerId);
            
            CustomerUpdateParams.Builder paramsBuilder = CustomerUpdateParams.builder();
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                paramsBuilder.putMetadata(entry.getKey(), entry.getValue());
            }

            Customer updatedCustomer = customer.update(paramsBuilder.build());
            log.info("Stripe 고객 메타데이터 업데이트 완료 - customerId: {}", customerId);
            return updatedCustomer;
        } catch (StripeException e) {
            log.error("Stripe 고객 메타데이터 업데이트 실패 - customerId: {}, error: {}", customerId, e.getMessage());
            throw new RuntimeException("Stripe 고객 메타데이터 업데이트 실패: " + e.getMessage(), e);
        }
    }
}