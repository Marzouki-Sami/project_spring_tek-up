package com.marzouki.shop.mail;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Lets only one email of a kind through per address within the cooldown. Kept in memory per instance. */
@Component
class SendThrottle {

    private final Cache<String, Boolean> recent;
    private final boolean off;

    SendThrottle(MailProperties properties) {
        this.off = properties.cooldown().isZero() || properties.cooldown().isNegative();
        this.recent = Caffeine.newBuilder().expireAfterWrite(off ? java.time.Duration.ofSeconds(1) : properties.cooldown())
                .maximumSize(50_000).build();
    }

    /** True when the email may be sent now; records the send. */
    boolean allow(String kind, String address) {
        if (off) {
            return true;
        }
        return recent.asMap().putIfAbsent(kind + "|" + address.toLowerCase(Locale.ROOT), Boolean.TRUE) == null;
    }
}