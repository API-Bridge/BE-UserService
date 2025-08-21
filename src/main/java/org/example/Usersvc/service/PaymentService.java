package org.example.Usersvc.service;

import org.example.Usersvc.domain.PlanType;
import java.util.Map;

/**
 * 결제 서비스 인터페이스
 */
public interface PaymentService {
    
    /**
     * 구독 결제 요청 생성
     */
    Map<String, Object> createSubscriptionPayment(String userId, PlanType planType);
    
    /**
     * 결제 승인 처리
     */
    Map<String, Object> confirmPayment(String paymentKey, String orderId, Integer amount);
    
    /**
     * 웹훅 처리
     */
    void processWebhook(Map<String, Object> webhookData);
    
    /**
     * 지원하는 결제 제공자 확인
     */
    String getProvider();
}