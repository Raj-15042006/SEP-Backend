package com.passport.config;

import com.passport.common.ratelimit.RateLimitResult;
import com.passport.common.ratelimit.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enterprise Token Bucket & Redis Rate Limiting HTTP Filter.
 * Protects backend endpoints against brute-force, credential stuffing, scraping, and DDoS attacks.
 * Seamlessly transitions between distributed Redis execution and local in-memory Token Buckets.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Bypass static assets, health probes, and API documentation
        if (isExemptPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIdentifier = resolveClientKey(request);
        String endpointTier = determineEndpointTier(request);

        RateLimitResult result = rateLimitService.checkRateLimit(clientIdentifier, endpointTier);

        // Standard HTTP Rate Limiting Headers (RFC 6585 & IETF draft)
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemainingTokens()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetSeconds()));
        response.setHeader("X-RateLimit-Algorithm", result.getAlgorithm());

        if (!result.isAllowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(result.getRetryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            String jsonPayload = String.format(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded (%d tokens/burst). Token bucket exhausted. Please retry in %d seconds.\",\"retryAfterSeconds\":%d,\"algorithm\":\"%s\"}",
                    result.getLimit(), result.getRetryAfterSeconds(), result.getRetryAfterSeconds(), result.getAlgorithm()
            );

            response.getWriter().write(jsonPayload);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExemptPath(String path) {
        return path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/info") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/favicon.ico");
    }

    private String determineEndpointTier(HttpServletRequest request) {
        String path = request.getRequestURI().toLowerCase();
        String method = request.getMethod().toUpperCase();

        if (path.contains("/auth") || path.contains("/login") || path.contains("/register") ||
            path.contains("/password") || path.contains("/recovery") || path.contains("/oauth")) {
            return "auth";
        }

        if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
            return "write";
        }

        return "api";
    }

    private String resolveClientKey(HttpServletRequest request) {
        // If authenticated user is present in context, bind to user ID to prevent IP hopping
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }

        // Otherwise rate limit by client IP address
        String clientIp = extractClientIp(request);
        return "ip:" + clientIp;
    }

    private String extractClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        boolean isTrustedProxy = remoteAddr != null && (
            remoteAddr.equals("127.0.0.1") ||
            remoteAddr.equals("0:0:0:0:0:0:0:1") ||
            remoteAddr.startsWith("10.") ||
            remoteAddr.startsWith("192.168.") ||
            remoteAddr.startsWith("172.")
        );

        if (isTrustedProxy) {
            String cfHeader = request.getHeader("CF-Connecting-IP");
            if (cfHeader != null && !cfHeader.isBlank()) {
                return cfHeader.trim();
            }
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.isBlank()) {
                return xfHeader.split(",")[0].trim();
            }
        }
        return remoteAddr != null ? remoteAddr : "127.0.0.1";
    }
}
