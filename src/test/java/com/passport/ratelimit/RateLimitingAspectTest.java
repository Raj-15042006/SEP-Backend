package com.passport.ratelimit;

import com.passport.common.ratelimit.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitingAspectTest {

    private RateLimitingAspect aspect;
    private RateLimitService rateLimitService;
    private InMemoryTokenBucketRateLimiter inMemoryLimiter;

    @BeforeEach
    void setUp() {
        inMemoryLimiter = new InMemoryTokenBucketRateLimiter();
        rateLimitService = new RateLimitService(inMemoryLimiter);
        ReflectionTestUtils.setField(rateLimitService, "enabled", true);

        aspect = new RateLimitingAspect(rateLimitService);
    }

    @Test
    @DisplayName("Method annotated with @RateLimited should allow requests up to capacity and throw RateLimitExceededException thereafter")
    void testRateLimitedAnnotationEnforcement() throws Throwable {
        RateLimited annotation = Mockito.mock(RateLimited.class);
        when(annotation.capacity()).thenReturn(2);
        when(annotation.refillRate()).thenReturn(0.1);
        when(annotation.tokens()).thenReturn(1);
        when(annotation.keyPrefix()).thenReturn("test-method");

        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);
        when(signature.toShortString()).thenReturn("TestService.execute()");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("SUCCESS");

        // Invocations 1 & 2 must succeed
        assertEquals("SUCCESS", aspect.enforceRateLimit(joinPoint, annotation));
        assertEquals("SUCCESS", aspect.enforceRateLimit(joinPoint, annotation));

        // 3rd invocation must throw RateLimitExceededException
        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> aspect.enforceRateLimit(joinPoint, annotation),
                "Exhausting annotated method capacity must throw RateLimitExceededException"
        );

        assertNotNull(exception.getResult());
        assertFalse(exception.getResult().isAllowed());
        assertTrue(exception.getResult().getRetryAfterSeconds() >= 1);
        assertTrue(exception.getMessage().contains("Rate limit exceeded"));
    }

    @Test
    @DisplayName("When rate limiting is disabled, method execution should bypass without checking tokens")
    void testRateLimiterDisabledBypass() throws Throwable {
        ReflectionTestUtils.setField(rateLimitService, "enabled", false);

        RateLimited annotation = Mockito.mock(RateLimited.class);
        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("BYPASS_OK");

        Object result = aspect.enforceRateLimit(joinPoint, annotation);
        assertEquals("BYPASS_OK", result);
        verify(joinPoint, times(1)).proceed();
    }
}
