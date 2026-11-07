package com.marzouki.shop.security.mfa;

import com.marzouki.shop.security.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Encrypts authenticator secrets at rest and hashes recovery codes and device tokens. */
@Slf4j
@Component
public class SecretVault {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    SecretVault(MfaProperties properties, JwtProperties jwt) {
        String material = properties.totpKey();
        if (material == null || material.isBlank()) {
            material = jwt.secret();
        }
        if (material == null || material.isBlank()) {
            log.warn("No app.mfa.totp-key or JWT secret is set: authenticator secrets cannot be read after a restart.");
            byte[] random = new byte[32];
            RANDOM.nextBytes(random);
            material = Base64.getEncoder().encodeToString(random);
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest((material + "|mfa").getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(digest, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            out.writeBytes(iv);
            out.writeBytes(encrypted);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public String decrypt(String stored) {
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, all, 0, 12));
            return new String(cipher.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("The stored authenticator secret cannot be read", e);
        }
    }

    /** A keyed hash, so a leaked table of hashes cannot be checked against guesses without the key. */
    public String hash(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}