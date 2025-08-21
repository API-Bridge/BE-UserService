package org.example.Usersvc.domain;

/**
 * 결제 제공자 enum
 */
public enum PaymentProvider {
    STRIPE("Stripe"),
    TOSSPAY("TossPay");
    
    private final String displayName;
    
    PaymentProvider(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}