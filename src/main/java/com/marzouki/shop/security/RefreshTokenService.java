package com.marzouki.shop.security;

import com.marzouki.shop.user.RefreshToken;
import com.marzouki.shop.user.RefreshTokenRepository;
import com.marzouki.shop.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Long-lived API credentials. Each token is random, stored only as a hash, and works once: using it
 * returns a new pair. Presenting a token that was already used or revoked is treated as theft and
 * signs the account out everywhere.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository tokens;
    private final JwtProperties properties;

    /** A newly issued token: the raw value is shown once, the hash is what gets stored. */
    public record Issued(String value, User user) {
    }

    @Transactional
    public Issued issue(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(value));
        token.setExpiresAt(Instant.now().plus(properties.refreshTtl()));
        tokens.save(token);
        return new Issued(value, user);
    }

    /** Exchanges a valid token for a new one. Returns the owner, or fails if it cannot be used. */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public Issued rotate(String presented) {
        RefreshToken token = tokens.findByTokenHash(hash(presented))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        User user = token.getUser();
        if (token.isRevoked()) {
            tokens.revokeAllForUser(user.getId());
            throw new BadCredentialsException("Invalid refresh token");
        }
        if (token.getExpiresAt().isBefore(Instant.now()) || !user.isEnabled()) {
            token.setRevoked(true);
            throw new BadCredentialsException("Invalid refresh token");
        }
        token.setRevoked(true);
        return issue(user);
    }

    @Transactional
    public void revoke(String presented) {
        tokens.findByTokenHash(hash(presented)).ifPresent(token -> token.setRevoked(true));
    }

    static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}