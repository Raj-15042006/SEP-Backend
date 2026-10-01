package com.passport.common.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outcome of a Token Bucket rate limit evaluation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResult {
    private boolean allowed;
    private long remainingTokens;
    private long resetSeconds;
    private long retryAfterSeconds;
    private int limit;
    private String algorithm;

    public static RateLimitResult allowed(int limit, long remainingTokens, long resetSeconds, String algorithm) {
        return RateLimitResult.builder()
                .allowed(true)
                .limit(limit)
                .remainingTokens(remainingTokens)
                .resetSeconds(resetSeconds)
                .retryAfterSeconds(0)
                .algorithm(algorithm)
                .build();
    }

    public static RateLimitResult rejected(int limit, long resetSeconds, long retryAfterSeconds, String algorithm) {
        return RateLimitResult.builder()
                .allowed(false)
                .limit(limit)
                .remainingTokens(0)
                .resetSeconds(resetSeconds)
                .retryAfterSeconds(retryAfterSeconds)
                .algorithm(algorithm)
                .build();
    }
}
