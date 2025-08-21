package org.example.Usersvc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TossPay 설정 정보
 */
@Data
@Component
@ConfigurationProperties(prefix = "tosspay")
public class TossPayProperties {
    
    private String clientKey;
    private String secretKey;
    private String securityKey;
    private String successUrl = "http://localhost:8081/api/payments/success";
    private String failUrl = "http://localhost:8081/api/payments/fail";
    private String confirmUrl = "https://api.tosspayments.com/v2/payments/confirm";
    
    private Prices prices = new Prices();
    
    @Data
    public static class Prices {
        private Integer proMonthly = 22; // 원 단위 (기존 하드코딩된 값)
        private Integer freeMonthly = 0;
    }
}