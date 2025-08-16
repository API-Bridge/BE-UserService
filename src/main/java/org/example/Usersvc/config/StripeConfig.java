package org.example.Usersvc.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class StripeConfig {

    private final StripeProperties stripeProperties;

    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = stripeProperties.getSecretKey();
        log.info("Stripe API key initialized");
    }
}