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
        private String enterprise;
    }

    @Data
    public static class Prices {
        private String proMonthly;
        private String proYearly;
        private String enterpriseMonthly;
        private String enterpriseYearly;
    }

    public String getPlanTypeByPriceId(String priceId) {
        if (priceId == null) {
            return "FREE";
        }

        if (priceId.equals(prices.getProMonthly()) || priceId.equals(prices.getProYearly())) {
            return "PRO";
        }

        if (priceId.equals(prices.getEnterpriseMonthly()) || priceId.equals(prices.getEnterpriseYearly())) {
            return "ENTERPRISE";
        }

        return "FREE";
    }

    public String getBillingPeriodByPriceId(String priceId) {
        if (priceId == null) {
            return "MONTHLY";
        }

        if (priceId.equals(prices.getProYearly()) || priceId.equals(prices.getEnterpriseYearly())) {
            return "YEARLY";
        }

        return "MONTHLY";
    }

    public String getProductIdByPlanType(String planType) {
        switch (planType.toUpperCase()) {
            case "PRO":
                return products.getPro();
            case "ENTERPRISE":
                return products.getEnterprise();
            case "FREE":
            default:
                return products.getFree();
        }
    }

    public String getMonthlyPriceIdByPlanType(String planType) {
        switch (planType.toUpperCase()) {
            case "PRO":
                return prices.getProMonthly();
            case "ENTERPRISE":
                return prices.getEnterpriseMonthly();
            default:
                return null;
        }
    }

    public String getYearlyPriceIdByPlanType(String planType) {
        switch (planType.toUpperCase()) {
            case "PRO":
                return prices.getProYearly();
            case "ENTERPRISE":
                return prices.getEnterpriseYearly();
            default:
                return null;
        }
    }
}