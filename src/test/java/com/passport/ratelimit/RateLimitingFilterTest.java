package com.passport.ratelimit;

import com.passport.common.ratelimit.InMemoryTokenBucketRateLimiter;
import com.passport.common.ratelimit.RateLimitService;
import com.passport.config.RateLimitingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;
    private RateLimitService rateLimitService;
    private InMemoryTokenBucketRateLimiter inMemoryLimiter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        inMemoryLimiter = new InMemoryTokenBucketRateLimiter();
        rateLimitService = new RateLimitService(inMemoryLimiter);

        ReflectionTestUtils.setField(rateLimitService, "enabled", true);
        ReflectionTestUtils.setField(rateLimitService, "defaultCapacity", 10);
        ReflectionTestUtils.setField(rateLimitService, "defaultRefillRate", 1.0);
        ReflectionTestUtils.setField(rateLimitService, "authCapacity", 3);
        ReflectionTestUtils.setField(rateLimitService, "authRefillRate", 0.1);

        rateLimitingFilter = new RateLimitingFilter(rateLimitService);
    }

    @Test
    @DisplayName("Allowed requests should attach RFC X-RateLimit headers and proceed down filter chain")
    void testAllowedRequestsHeadersAndChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/evidence");
        request.setRemoteAddr("198.51.100.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = Mockito.mock(FilterChain.class);

        rateLimitingFilter.doFilter(request, response, filterChain);

        // Verify downstream filter was called
        Mockito.verify(filterChain, Mockito.times(1)).doFilter(request, response);

        // Verify headers
        assertEquals(200, response.getStatus());
        assertEquals("10", response.getHeader("X-RateLimit-Limit"));
        assertEquals("9", response.getHeader("X-RateLimit-Remaining"));
        assertNotNull(response.getHeader("X-RateLimit-Reset"));
        assertEquals("IN_MEMORY_TOKEN_BUCKET", response.getHeader("X-RateLimit-Algorithm"));
    }

    @Test
    @DisplayName("Exhausting auth tier capacity should return HTTP 429 and block filter chain")
    void testAuthTierDepletedReturns429() throws ServletException, IOException {
        String clientIp = "198.51.100.42";

        // Consume all 3 auth tokens
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            rateLimitingFilter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus());
            assertEquals(String.valueOf(2 - i), res.getHeader("X-RateLimit-Remaining"));
        }

        // 4th request must be rejected with 429
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        blockedReq.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        FilterChain blockedChain = Mockito.mock(FilterChain.class);

        rateLimitingFilter.doFilter(blockedReq, blockedRes, blockedChain);

        // Ensure downstream controllers are NOT invoked
        Mockito.verify(blockedChain, Mockito.never()).doFilter(blockedReq, blockedRes);

        // Validate 429 response structure
        assertEquals(429, blockedRes.getStatus());
        assertNotNull(blockedRes.getHeader("Retry-After"));
        assertTrue(blockedRes.getContentAsString().contains("\"status\":429"));
        assertTrue(blockedRes.getContentAsString().contains("Too Many Requests"));
        assertTrue(blockedRes.getContentAsString().contains("Token bucket exhausted"));
    }

    @Test
    @DisplayName("Exempt paths such as health checks and swagger should bypass rate limiting")
    void testExemptPathsBypassFilter() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = Mockito.mock(FilterChain.class);

        rateLimitingFilter.doFilter(request, response, filterChain);

        Mockito.verify(filterChain, Mockito.times(1)).doFilter(request, response);
        assertNull(response.getHeader("X-RateLimit-Limit"), "Exempt paths should not have rate limit headers");
    }

    @Test
    @DisplayName("Trusted proxy should extract client IP from CF-Connecting-IP")
    void testTrustedProxyIpExtraction() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/skills");
        request.setRemoteAddr("127.0.0.1"); // Trusted local gateway
        request.addHeader("CF-Connecting-IP", "203.0.113.195");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        rateLimitingFilter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("9", response.getHeader("X-RateLimit-Remaining"));
    }

    @Test
    @DisplayName("Authenticated users should bind to user ID to prevent IP rotation bypass")
    void testAuthenticatedUserBinding() throws ServletException, IOException {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-uuid-12345", "cred",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT")))
        );

        // Consume tokens using one IP
        MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/api/v1/evidence");
        req1.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(req1, res1, new MockFilterChain());

        // Same user from a different IP should share the same token bucket
        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/v1/evidence");
        req2.setRemoteAddr("10.10.10.10"); // Rotated IP
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(req2, res2, new MockFilterChain());

        // Token count should decrease across different IPs for the same user
        assertEquals("9", res1.getHeader("X-RateLimit-Remaining"));
        assertEquals("8", res2.getHeader("X-RateLimit-Remaining"));
    }

    @Test
    @DisplayName("Concurrent multi-threaded requests must decrement tokens with thread safety")
    void testConcurrentMultiThreadedTokenConsumption() throws InterruptedException {
        int threads = 20;
        int capacity = 15;
        ReflectionTestUtils.setField(rateLimitService, "defaultCapacity", capacity);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        String clientIp = "192.0.2.1";

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/portfolio/1");
                    req.setRemoteAddr(clientIp);
                    MockHttpServletResponse res = new MockHttpServletResponse();
                    rateLimitingFilter.doFilter(req, res, new MockFilterChain());

                    if (res.getStatus() == 200) {
                        allowedCount.incrementAndGet();
                    } else if (res.getStatus() == 429) {
                        rejectedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    fail("Exception in thread: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Exactly 15 requests should be allowed and 5 rejected
        assertEquals(capacity, allowedCount.get(), "Allowed count must match token bucket capacity");
        assertEquals(threads - capacity, rejectedCount.get(), "Excess requests must be rejected with 429");
    }
}
