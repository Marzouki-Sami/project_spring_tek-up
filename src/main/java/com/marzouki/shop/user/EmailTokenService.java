package com.marzouki.shop.user;

import com.marzouki.shop.common.ConflictException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** One-time links sent by email. Only a hash is stored; the raw value exists in the email alone. */
@Service
@RequiredArgsConstructor
public class EmailTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    static final Duration VERIFY_TTL = Duration.ofDays(2);
    static final Duration RESET_TTL = Duration.ofHours(1);

    private final EmailTokenRepository tokens;

    @Transactional
    public String issue(User user, EmailToken.Type type) {
        tokens.retireOpen(user.getId(), type, Instant.now());
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        EmailToken token = new EmailToken();
        token.setUser(user);
        token.setType(type);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(type == EmailToken.Type.VERIFY ? VERIFY_TTL : RESET_TTL));
        tokens.save(token);
        return raw;
    }

    /** Marks the token used and returns its owner, or fails if it is unknown, used or expired. */
    @Transactional
    public User consume(String raw, EmailToken.Type type) {
        EmailToken token = tokens.findByTokenHashAndType(hash(raw == null ? "" : raw), type)
                .orElseThrow(() -> new ConflictException("This link is not valid"));
        if (token.getUsedAt() != null) {
            throw new ConflictException("This link has already been used");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ConflictException("This link has expired. Please request a new one");
        }
        token.setUsedAt(Instant.now());
        return token.getUser();
    }

    static String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}