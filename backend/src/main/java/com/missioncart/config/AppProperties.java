package com.missioncart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "missioncart")
public record AppProperties(
    Jwt jwt,
    Cors cors,
    Ai ai,
    Razorpay razorpay,
    Admin admin
) {
    public record Jwt(String secret, String refreshSecret, long accessExpirationMs, long refreshExpirationMs, boolean cookieSecure) {}
    public record Cors(String allowedOrigins) {}
    public record Ai(String provider, String geminiApiKey, String openaiApiKey) {}
    public record Razorpay(String keyId, String keySecret, String webhookSecret) {}
    public record Admin(String bootstrapEmail, String bootstrapPassword) {}
}
