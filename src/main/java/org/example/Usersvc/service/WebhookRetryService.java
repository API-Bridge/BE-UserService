package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 웹훅 재시도 처리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookRetryService {

    private final PaymentServiceFactory paymentServiceFactory;

    /**
     * 재시도가 가능한 웹훅 처리
     * 최대 3번 재시도, 각 재시도 간격은 2초, 4초, 8초
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void processWebhookWithRetry(String provider, Map<String, Object> webhookData) {
        log.info("웹훅 처리 시작 - provider: {}, 재시도 가능", provider);
        
        try {
            PaymentService paymentService = paymentServiceFactory.getPaymentService(provider);
            paymentService.processWebhook(webhookData);
            
            log.info("✅ 웹훅 처리 성공 - provider: {}", provider);
            
        } catch (Exception e) {
            log.error("❌ 웹훅 처리 실패 - provider: {}, error: {}", provider, e.getMessage());
            throw e; // 재시도를 위해 예외를 다시 던짐
        }
    }

    /**
     * 재시도 최종 실패 시 호출되는 메소드
     */
    public void handleWebhookFailure(String provider, Map<String, Object> webhookData, Exception ex) {
        log.error("🚨 웹훅 처리 최종 실패 - provider: {}, 모든 재시도 소진됨", provider);
        log.error("실패한 웹훅 데이터: {}", webhookData);
        log.error("최종 오류: {}", ex.getMessage(), ex);
        
        // 운영 환경에서는 실패한 웹훅을 Dead Letter Queue에 저장하고 알림 발송 필요
        // 예: 데이터베이스에 실패 로그 저장, 슬랙/이메일 알림, 모니터링 시스템 연동
    }
}