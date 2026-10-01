package com.passport.common.ratelimit;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Distributed Token Bucket Rate Limiter powered by Redis & Atomic Lua Scripting.
 * Provides microsecond atomicity across distributed cluster nodes with resilient
 * automatic fallback to in-memory Token Bucket when Redis is unavailable.
 */
@Slf4j
@Primary
@Component("redisTokenBucketRateLimiter")
@RequiredArgsConstructor
public class RedisTokenBucketRateLimiter implements TokenBucketRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;
    private final InMemoryTokenBucketRateLimiter inMemoryFallback;

    @Value("${ratelimit.use-redis:true}")
    private boolean useRedis;

    private final AtomicBoolean redisAvailable = new AtomicBoolean(true);
    private DefaultRedisScript<List> redisScript;

    private static final String TOKEN_BUCKET_LUA_SCRIPT =
            "local key = KEYS[1]\n" +
            "local capacity = tonumber(ARGV[1])\n" +
            "local refillRate = tonumber(ARGV[2])\n" +
            "local now = tonumber(ARGV[3])\n" +
            "local requested = tonumber(ARGV[4])\n" +
            "\n" +
            "local data = redis.call('HMGET', key, 'tokens', 'lastRefill')\n" +
            "local currentTokens = tonumber(data[1])\n" +
            "local lastRefill = tonumber(data[2])\n" +
            "\n" +
            "if currentTokens == nil then\n" +
            "    currentTokens = capacity\n" +
            "    lastRefill = now\n" +
            "else\n" +
            "    local delta = math.max(0, now - lastRefill)\n" +
            "    currentTokens = math.min(capacity, currentTokens + (delta * refillRate))\n" +
            "    lastRefill = now\n" +
            "end\n" +
            "\n" +
            "local allowed = 0\n" +
            "local retryAfter = 0\n" +
            "\n" +
            "if currentTokens >= requested then\n" +
            "    currentTokens = currentTokens - requested\n" +
            "    allowed = 1\n" +
            "else\n" +
            "    local missing = requested - currentTokens\n" +
            "    retryAfter = math.ceil(missing / refillRate)\n" +
            "end\n" +
            "\n" +
            "redis.call('HMSET', key, 'tokens', currentTokens, 'lastRefill', lastRefill)\n" +
            "local ttl = math.max(60, math.ceil(capacity / refillRate) * 2)\n" +
            "redis.call('EXPIRE', key, ttl)\n" +
            "\n" +
            "local resetTime = math.ceil((capacity - currentTokens) / refillRate)\n" +
            "return { allowed, math.floor(currentTokens), resetTime, retryAfter }";

    @PostConstruct
    public void init() {
        redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(TOKEN_BUCKET_LUA_SCRIPT);
        redisScript.setResultType(List.class);
    }

    @Override
    public RateLimitResult tryConsume(String key, int tokensRequested, int capacity, double refillRatePerSec) {
        if (!useRedis || !redisAvailable.get()) {
            return inMemoryFallback.tryConsume(key, tokensRequested, capacity, refillRatePerSec);
        }

        try {
            String redisKey = "ratelimit:token_bucket:" + key;
            long nowEpochSeconds = Instant.now().getEpochSecond();

            List<?> result = stringRedisTemplate.execute(
                    redisScript,
                    Collections.singletonList(redisKey),
                    String.valueOf(capacity),
                    String.valueOf(refillRatePerSec),
                    String.valueOf(nowEpochSeconds),
                    String.valueOf(tokensRequested)
            );

            if (result != null && result.size() >= 4) {
                long allowed = ((Number) result.get(0)).longValue();
                long remainingTokens = ((Number) result.get(1)).longValue();
                long resetSeconds = ((Number) result.get(2)).longValue();
                long retryAfterSeconds = ((Number) result.get(3)).longValue();

                if (allowed == 1) {
                    return RateLimitResult.allowed(capacity, remainingTokens, resetSeconds, "REDIS_TOKEN_BUCKET");
                } else {
                    return RateLimitResult.rejected(capacity, resetSeconds, retryAfterSeconds, "REDIS_TOKEN_BUCKET");
                }
            }
        } catch (Exception e) {
            if (redisAvailable.compareAndSet(true, false)) {
                log.warn("Redis rate limiter unavailable ({}), smoothly falling back to resilient in-memory Token Bucket engine.", e.getMessage());
            }
        }

        return inMemoryFallback.tryConsume(key, tokensRequested, capacity, refillRatePerSec);
    }

    public void setRedisAvailable(boolean available) {
        this.redisAvailable.set(available);
    }

    public boolean isRedisActive() {
        return useRedis && redisAvailable.get();
    }
}
