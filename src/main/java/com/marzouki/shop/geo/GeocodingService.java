package com.marzouki.shop.geo;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Address search with a cache and a polite pace towards the provider (free services allow about one
 * request per second). Identical searches within an hour never reach the provider twice.
 */
@Service
public class GeocodingService {

    private static final long MIN_GAP_MILLIS = 1100;
    private static final long MAX_WAIT_MILLIS = 3500;

    private final GeocodingClient client;
    private final Cache<String, List<GeoPlace>> searches = Caffeine.newBuilder()
            .maximumSize(2_000).expireAfterWrite(Duration.ofHours(1)).build();
    private final Cache<String, Optional<GeoPlace>> reverses = Caffeine.newBuilder()
            .maximumSize(2_000).expireAfterWrite(Duration.ofHours(1)).build();
    private final java.util.concurrent.atomic.AtomicLong nextSlot = new java.util.concurrent.atomic.AtomicLong();

    GeocodingService(ObjectProvider<GeocodingClient> client) {
        this.client = client.getIfAvailable();
    }

    public boolean isAvailable() {
        return client != null;
    }

    public List<GeoPlace> search(String query) {
        String key = query.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return searches.get(key, k -> throttled(() -> client.search(query.trim())));
    }

    public Optional<GeoPlace> reverse(double lat, double lon) {
        String key = String.format(Locale.ROOT, "%.5f,%.5f", lat, lon);
        return reverses.get(key, k -> throttled(() -> client.reverse(lat, lon)));
    }

    private <T> T throttled(java.util.function.Supplier<T> call) {
        long now = System.currentTimeMillis();
        long slot = nextSlot.updateAndGet(previous -> Math.max(now, previous) + MIN_GAP_MILLIS);
        long wait = slot - MIN_GAP_MILLIS - now;
        if (wait > MAX_WAIT_MILLIS) {
            // Too many searches queued behind the provider's pace: fail fast instead of tying up request threads.
            nextSlot.addAndGet(-MIN_GAP_MILLIS);
            throw new GeocodingUnavailableException("Address search is busy");
        }
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return call.get();
    }
}