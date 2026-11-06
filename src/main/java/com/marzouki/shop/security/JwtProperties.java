package com.marzouki.shop.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for API tokens.
 *
 * @param secret   HMAC signing key, at least 32 characters. When empty a random key is generated at
 *                 startup, which is fine for development but invalidates tokens on every restart.
 * @param accessTtl how long an access token is valid
 * @param refreshTtl how long a refresh token is valid
 * @param issuer   the "iss" claim, checked when tokens are read back
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration accessTtl, Duration refreshTtl, String issuer) {

    public JwtProperties {
        accessTtl = accessTtl == null ? Duration.ofMinutes(15) : accessTtl;
        refreshTtl = refreshTtl == null ? Duration.ofDays(7) : refreshTtl;
        issuer = issuer == null || issuer.isBlank() ? "spring-shop" : issuer;
    }
}