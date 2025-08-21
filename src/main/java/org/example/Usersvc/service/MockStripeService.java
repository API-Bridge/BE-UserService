package org.example.Usersvc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Mock Stripe Service for Development and Testing
 * 실제 Stripe API 키가 설정되지 않은 경우 사용되는 Mock 서비스
 */
// @Service  // 비활성화 - 토스페이로 대체됨
@Profile("dev")
@Slf4j
public class MockStripeService {

    /**
     * Mock 고객 생성
     */
    public String createMockCustomer(String email, String name) {
        log.info("[MOCK] Creating customer - email: {}, name: {}", email, name);
        
        // Mock Customer ID 생성
        String customerId = "cus_mock_" + UUID.randomUUID().toString().substring(0, 8);
        
        log.info("[MOCK] Customer created successfully - customerId: {}", customerId);
        return customerId;
    }

    /**
     * Mock 구독 생성
     */
    public String createMockSubscription(String customerId, String priceId) {
        log.info("[MOCK] Creating subscription - customerId: {}, priceId: {}", customerId, priceId);
        
        // Mock Subscription ID 생성
        String subscriptionId = "sub_mock_" + UUID.randomUUID().toString().substring(0, 8);
        
        log.info("[MOCK] Subscription created successfully - subscriptionId: {}", subscriptionId);
        return subscriptionId;
    }

    /**
     * Mock 구독 취소
     */
    public boolean cancelMockSubscription(String subscriptionId) {
        log.info("[MOCK] Cancelling subscription - subscriptionId: {}", subscriptionId);
        
        // 항상 성공으로 처리
        log.info("[MOCK] Subscription cancelled successfully");
        return true;
    }

    /**
     * 실제 Stripe API 키가 유효한지 확인
     */
    public boolean isValidStripeKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }
        
        // Mock 키 또는 실제 키 형식이 아닌 경우 false
        if (apiKey.contains("mock") || apiKey.contains("dev") || 
            (!apiKey.startsWith("sk_test_") && !apiKey.startsWith("sk_live_"))) {
            return false;
        }
        
        return true;
    }
}
