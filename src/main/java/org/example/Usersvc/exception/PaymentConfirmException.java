package org.example.Usersvc.exception;

/**
 * 결제 승인 실패 예외
 */
public class PaymentConfirmException extends PaymentException {
    
    public PaymentConfirmException(String message) {
        super(message, "PAYMENT_CONFIRM_FAILED");
    }
    
    public PaymentConfirmException(String message, Throwable cause) {
        super(message, "PAYMENT_CONFIRM_FAILED", cause);
    }
}