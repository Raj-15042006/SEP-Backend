package com.passport.common.ratelimit;

/**
 * Token Bucket Rate Limiter interface for burst-tolerant, smoothly refilling throttling.
 */
public interface TokenBucketRateLimiter {

    /**
     * Attempts to consume tokens from the bucket identified by key.
     *
     * @param key              Unique identifier (client IP, user ID, API action)
     * @param tokensRequested  Number of tokens to consume (usually 1)
     * @param capacity         Maximum burst capacity of the token bucket
     * @param refillRatePerSec Rate at which tokens are added back to the bucket each second
     * @return RateLimitResult containing consumption decision, remaining tokens, and reset times
     */
    RateLimitResult tryConsume(String key, int tokensRequested, int capacity, double refillRatePerSec);
}
