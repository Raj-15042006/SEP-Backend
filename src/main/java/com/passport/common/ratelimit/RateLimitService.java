package com.passport.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Enterprise Rate Limit Service orchestrating Token Bucket evaluation
 * across Redis and fallback memory stores with tiered role and endpoint limits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final TokenBucketRateLimiter rateLimiter;

    @Value("${ratelimit.enabled:true}")
    private boolean enabled;

    @Value("${ratelimit.default-capacity:120}")
    private int defaultCapacity;

    @Value("${ratelimit.default-refill-rate:2.0}")
    private double defaultRefillRate;

    @Value("${ratelimit.auth-capacity:20}")
    private int authCapacity;

    @Value("${ratelimit.auth-refill-rate:0.333}")
    private double authRefillRate;

    /**
     * Evaluates rate limiting for an HTTP request based on client key and endpoint tier.
     *
     * @param clientIdentifier IP address, user UUID, or API key
     * @param endpointTier     "auth" for login/passwords, "write" for evidence submissions, "api" for standard reads
     * @return RateLimitResult with decision and header metadata
     */
    public RateLimitResult checkRateLimit(String clientIdentifier, String endpointTier) {
        if (!enabled) {
            return RateLimitResult.allowed(1000, 1000, 0, "DISABLED");
        }

        int capacity;
        double refillRate;

        if ("auth".equalsIgnoreCase(endpointTier)) {
            capacity = authCapacity;
            refillRate = authRefillRate;
        } else if ("write".equalsIgnoreCase(endpointTier)) {
            capacity = Math.min(defaultCapacity, 40);
            refillRate = 1.0;
        } else {
            capacity = defaultCapacity;
            refillRate = defaultRefillRate;
        }

        String bucketKey = endpointTier + ":" + clientIdentifier;
        return rateLimiter.tryConsume(bucketKey, 1, capacity, refillRate);
    }

    /**
     * Custom token bucket check for annotated methods or specific business operations.
     */
    public RateLimitResult checkCustomLimit(String key, int tokensRequested, int capacity, double refillRatePerSec) {
        if (!enabled) {
            return RateLimitResult.allowed(capacity, capacity, 0, "DISABLED");
        }
        return rateLimiter.tryConsume(key, tokensRequested, capacity, refillRatePerSec);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
