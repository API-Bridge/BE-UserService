package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import org.example.Usersvc.domain.PaymentProvider;
import org.springframework.stereotype.Component;

/**
 * 결제 서비스 팩토리
 */
@Component
@RequiredArgsConstructor
public class PaymentServiceFactory {

    private final TossPaymentService tossPaymentService;
    // private final StripePaymentService stripePaymentService; // 향후 구현
    
    /**
     * 결제 제공자별 서비스 반환
     */
    public PaymentService getPaymentService(String provider) {
        switch (provider.toUpperCase()) {
            case "TOSSPAY":
                return tossPaymentService;
            case "STRIPE":
                // return stripePaymentService; // 향후 구현
                throw new IllegalArgumentException("Stripe 서비스는 현재 비활성화되었습니다.");
            default:
                throw new IllegalArgumentException("지원하지 않는 결제 제공자입니다: " + provider);
        }
    }
    
    /**
     * PaymentProvider enum으로 서비스 반환
     */
    public PaymentService getPaymentService(PaymentProvider provider) {
        return getPaymentService(provider.name());
    }
}