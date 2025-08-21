package org.example.Usersvc.service;

import lombok.extern.slf4j.Slf4j;

/**
 * Stripe Webhook 서비스 (TossPay 전환으로 비활성화됨)
 */
@Slf4j
// @Service - TossPay 전환으로 비활성화
public class StripeWebhookService {
    
    // Stripe 관련 모든 기능이 TossPay로 전환되어 비활성화됨
    // 이 클래스는 호환성 유지를 위해 남겨둠
    
    public StripeWebhookService() {
        log.info("StripeWebhookService가 로드되었지만 TossPay 전환으로 비활성화 상태입니다.");
    }
}