package com.passport.ratelimit;

import com.passport.common.ratelimit.InMemoryTokenBucketRateLimiter;
import com.passport.common.ratelimit.RateLimitResult;
import com.passport.common.ratelimit.RateLimitService;
import com.passport.common.ratelimit.RedisTokenBucketRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketRateLimiterTest {

    private InMemoryTokenBucketRateLimiter inMemoryLimiter;
    private RedisTokenBucketRateLimiter redisLimiter;
    private StringRedisTemplate mockRedisTemplate;

    @BeforeEach
    void setUp() {
        inMemoryLimiter = new InMemoryTokenBucketRateLimiter();
        mockRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        redisLimiter = new RedisTokenBucketRateLimiter(mockRedisTemplate, inMemoryLimiter);
        redisLimiter.init();
    }

    @Test
    @DisplayName("Should allow burst requests up to exact capacity and reject subsequent request")
    void testBurstCapacityAndRejection() {
        int capacity = 5;
        double refillRate = 1.0;
        String clientKey = "test-client-burst";

        // First 5 requests must be allowed
        for (int i = 0; i < capacity; i++) {
            RateLimitResult result = inMemoryLimiter.tryConsume(clientKey, 1, capacity, refillRate);
            assertTrue(result.isAllowed(), "Request " + (i + 1) + " should be allowed");
            assertEquals(capacity - 1 - i, result.getRemainingTokens());
        }

        // 6th request must be rejected
        RateLimitResult rejectedResult = inMemoryLimiter.tryConsume(clientKey, 1, capacity, refillRate);
        assertFalse(rejectedResult.isAllowed(), "6th request should exceed token bucket capacity");
        assertEquals(0, rejectedResult.getRemainingTokens());
        assertTrue(rejectedResult.getRetryAfterSeconds() >= 1, "Retry after should be at least 1 second");
    }

    @Test
    @DisplayName("Should smoothly refill tokens after elapsed time")
    void testTokenRefillOverTime() throws InterruptedException {
        int capacity = 2;
        double refillRate = 4.0; // 4 tokens per second (1 token every 250ms)
        String clientKey = "test-client-refill";

        // Deplete all 2 tokens
        assertTrue(inMemoryLimiter.tryConsume(clientKey, 1, capacity, refillRate).isAllowed());
        assertTrue(inMemoryLimiter.tryConsume(clientKey, 1, capacity, refillRate).isAllowed());
        assertFalse(inMemoryLimiter.tryConsume(clientKey, 1, capacity, refillRate).isAllowed());

        // Wait ~300ms for at least 1 token to refill
        Thread.sleep(300);

        RateLimitResult refilledResult = inMemoryLimiter.tryConsume(clientKey, 1, capacity, refillRate);
        assertTrue(refilledResult.isAllowed(), "Token bucket should have refilled after time elapsed");
    }

    @Test
    @DisplayName("Redis limiter should gracefully fallback to in-memory engine when Redis is unavailable")
    void testRedisFallbackToInMemory() {
        // Mock Redis throwing an exception
        Mockito.when(mockRedisTemplate.execute(Mockito.any(), Mockito.anyList(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new RuntimeException("Redis connection refused"));

        RateLimitResult result = redisLimiter.tryConsume("fallback-client", 1, 10, 1.0);
        assertNotNull(result);
        assertTrue(result.isAllowed());
        assertEquals("IN_MEMORY_TOKEN_BUCKET", result.getAlgorithm());
    }

    @Test
    @DisplayName("RateLimitService applies tiered capacities for auth and api endpoints")
    void testRateLimitServiceTiers() {
        RateLimitService service = new RateLimitService(inMemoryLimiter);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "defaultCapacity", 120);
        ReflectionTestUtils.setField(service, "defaultRefillRate", 2.0);
        ReflectionTestUtils.setField(service, "authCapacity", 20);
        ReflectionTestUtils.setField(service, "authRefillRate", 0.333);

        RateLimitResult authResult = service.checkRateLimit("client-1", "auth");
        assertEquals(20, authResult.getLimit());

        RateLimitResult apiResult = service.checkRateLimit("client-1", "api");
        assertEquals(120, apiResult.getLimit());
    }
}
