package com.marzouki.shop.security.mfa;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Two-factor settings.
 *
 * @param staff2fa  managers and admins must use an authenticator app
 * @param totpKey   key that protects stored authenticator secrets; defaults to a key derived from the JWT secret
 * @param trustDays how long "trust this device" skips the code
 * @param issuer    name shown in the authenticator app
 */
@ConfigurationProperties(prefix = "app.mfa")
public record MfaProperties(Boolean staff2fa, String totpKey, Integer trustDays, String issuer) {

    public MfaProperties {
        staff2fa = staff2fa == null || staff2fa;
        trustDays = trustDays == null ? 30 : trustDays;
        issuer = issuer == null || issuer.isBlank() ? "Shop" : issuer;
    }
}