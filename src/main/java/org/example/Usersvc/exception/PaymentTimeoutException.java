package org.example.Usersvc.exception;

/**
 * 결제 시간 초과 예외 (10분 내 confirm 호출 실패 등)
 */
public class PaymentTimeoutException extends PaymentException {
    
    public PaymentTimeoutException(String message) {
        super(message, "PAYMENT_TIMEOUT");
    }
    
    public PaymentTimeoutException(String message, Throwable cause) {
        super(message, "PAYMENT_TIMEOUT", cause);
    }
}