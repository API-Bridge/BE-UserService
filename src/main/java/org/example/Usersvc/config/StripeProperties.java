package org.example.Usersvc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    private String secretKey;
    private String publicKey;
    private String webhookSecret;
    private Products products = new Products();
    private Prices prices = new Prices();

    @Data
    public static class Products {
        private String free;
        private String pro;
    }

    @Data
    public static class Prices {
        private String proMonthly;
    }

    public String getPlanNameByPriceId(String priceId) {
        if (priceId == null) {
            return "FREE";
        }

        if (priceId.equals(prices.getProMonthly())) {
            return "PRO";
        }

        return "FREE";
    }

    public String getBillingPeriodByPriceId(String priceId) {
        // 현재는 Monthly만 지원
        return "MONTHLY";
    }

    public String getProductIdByPlanName(String planName) {
        switch (planName.toUpperCase()) {
            case "PRO":
                return products.getPro();
            case "FREE":
            default:
                return products.getFree();
        }
    }

    public String getMonthlyPriceIdByPlanName(String planName) {
        switch (planName.toUpperCase()) {
            case "PRO":
                return prices.getProMonthly();
            default:
                return null;
        }
    }

}