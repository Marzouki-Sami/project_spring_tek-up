package com.marzouki.shop.security.mfa;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.marzouki.shop.common.ConflictException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** Stops guessing of codes: five wrong tries block further tries for ten minutes. Kept in memory per instance. */
@Component
class MfaAttempts {

    private static final int MAX_FAILURES = 5;

    private final Cache<Long, AtomicInteger> failures = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10)).maximumSize(10_000).build();

    void check(Long userId) {
        AtomicInteger count = failures.getIfPresent(userId);
        if (count != null && count.get() >= MAX_FAILURES) {
            throw new ConflictException("Too many wrong codes. Wait a few minutes and try again.");
        }
    }

    void fail(Long userId) {
        failures.get(userId, k -> new AtomicInteger()).incrementAndGet();
    }

    void clear(Long userId) {
        failures.invalidate(userId);
    }
}