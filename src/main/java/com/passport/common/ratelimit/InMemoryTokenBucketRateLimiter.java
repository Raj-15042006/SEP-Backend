package com.passport.common.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * High-performance In-Memory Token Bucket Rate Limiter.
 * Uses nanosecond resolution for smooth token regeneration and zero external dependencies.
 * Acts as a primary local engine or resilient fallback when Redis is unreachable.
 */
@Slf4j
@Component("inMemoryTokenBucketRateLimiter")
public class InMemoryTokenBucketRateLimiter implements TokenBucketRateLimiter {

    private static class Bucket {
        final int capacity;
        final double refillRatePerSec;
        double tokens;
        long lastRefillNanos;

        Bucket(int capacity, double refillRatePerSec) {
            this.capacity = capacity;
            this.refillRatePerSec = refillRatePerSec;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized RateLimitResult consume(int requested) {
            long now = System.nanoTime();
            long elapsedNanos = now - lastRefillNanos;
            double elapsedSeconds = (double) elapsedNanos / TimeUnit.SECONDS.toNanos(1);

            // Refill tokens based on elapsed time up to maximum capacity
            tokens = Math.min(capacity, tokens + (elapsedSeconds * refillRatePerSec));
            lastRefillNanos = now;

            if (tokens >= requested) {
                tokens -= requested;
                long resetSeconds = Math.max(1, (long) Math.ceil((capacity - tokens) / refillRatePerSec));
                return RateLimitResult.allowed(capacity, (long) Math.floor(tokens), resetSeconds, "IN_MEMORY_TOKEN_BUCKET");
            } else {
                double missingTokens = requested - tokens;
                long retryAfterSeconds = Math.max(1, (long) Math.ceil(missingTokens / refillRatePerSec));
                long resetSeconds = Math.max(1, (long) Math.ceil((capacity - tokens) / refillRatePerSec));
                return RateLimitResult.rejected(capacity, resetSeconds, retryAfterSeconds, "IN_MEMORY_TOKEN_BUCKET");
            }
        }
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult tryConsume(String key, int tokensRequested, int capacity, double refillRatePerSec) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, refillRatePerSec));
        return bucket.consume(tokensRequested);
    }

    /**
     * Periodic cleanup for inactive buckets to maintain zero memory leak profile
     */
    public void cleanupInactive(long maxIdleSeconds) {
        long thresholdNanos = System.nanoTime() - TimeUnit.SECONDS.toNanos(maxIdleSeconds);
        buckets.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                return entry.getValue().lastRefillNanos < thresholdNanos && entry.getValue().tokens >= entry.getValue().capacity;
            }
        });
    }

    public int getActiveBucketsCount() {
        return buckets.size();
    }
}
