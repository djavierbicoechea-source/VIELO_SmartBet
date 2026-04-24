package com.vielo.smartbet.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vielo.stripe")
public record StripeProperties(
        boolean enabled,
        String secretKey,
        String publicKey,
        String currency,
        String successUrl,
        String cancelUrl
) {
}
