package org.example.Usersvc.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "toss")
@Component
public class TossPayProperties {
    
    private String clientKey;
    private String secretKey;
    private String successUrl;
    private String failUrl;
    private String confirmUrl;
    private Prices prices;
    
    // getter, setter
    public String getClientKey() {
        return clientKey;
    }
    
    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }
    
    public String getSecretKey() {
        return secretKey;
    }
    
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    public String getSuccessUrl() {
        return successUrl;
    }
    
    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }
    
    public String getFailUrl() {
        return failUrl;
    }
    
    public void setFailUrl(String failUrl) {
        this.failUrl = failUrl;
    }
    
    public String getConfirmUrl() {
        return confirmUrl;
    }
    
    public void setConfirmUrl(String confirmUrl) {
        this.confirmUrl = confirmUrl;
    }
    
    public Prices getPrices() {
        return prices;
    }
    
    public void setPrices(Prices prices) {
        this.prices = prices;
    }
    
    public static class Prices {
        private Integer proMonthly;
        private Integer freeMonthly;
        
        public Integer getProMonthly() {
            return proMonthly;
        }
        
        public void setProMonthly(Integer proMonthly) {
            this.proMonthly = proMonthly;
        }
        
        public Integer getFreeMonthly() {
            return freeMonthly;
        }
        
        public void setFreeMonthly(Integer freeMonthly) {
            this.freeMonthly = freeMonthly;
        }
    }
}