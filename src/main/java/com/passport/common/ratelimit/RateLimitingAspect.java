package com.passport.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AOP Aspect intercepting methods annotated with @RateLimited
 * to enforce discrete Token Bucket rate limits with Redis/In-Memory execution.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitingAspect {

    private final RateLimitService rateLimitService;

    @Around("@annotation(rateLimited)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        if (!rateLimitService.isEnabled()) {
            return joinPoint.proceed();
        }

        String clientIdentifier = resolveClientIdentifier();
        String methodName = joinPoint.getSignature().toShortString();
        String bucketKey = rateLimited.keyPrefix() + ":" + methodName + ":" + clientIdentifier;

        RateLimitResult result = rateLimitService.checkCustomLimit(
                bucketKey,
                rateLimited.tokens(),
                rateLimited.capacity(),
                rateLimited.refillRate()
        );

        // Attach rate limit headers to current HTTP response if available
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletResponse response = attributes.getResponse();
            if (response != null) {
                response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
                response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemainingTokens()));
                response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetSeconds()));
                response.setHeader("X-RateLimit-Algorithm", result.getAlgorithm());
            }
        }

        if (!result.isAllowed()) {
            log.warn("Rate limit exceeded for client '{}' on method '{}' (Bucket: {}). Retry after: {}s",
                    clientIdentifier, methodName, bucketKey, result.getRetryAfterSeconds());

            if (attributes != null && attributes.getResponse() != null) {
                attributes.getResponse().setHeader("Retry-After", String.valueOf(result.getRetryAfterSeconds()));
            }

            throw new RateLimitExceededException(
                    String.format("Rate limit exceeded (%d burst capacity). Token bucket depleted. Please retry in %d seconds.",
                            result.getLimit(), result.getRetryAfterSeconds()),
                    result
            );
        }

        return joinPoint.proceed();
    }

    private String resolveClientIdentifier() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String remoteAddr = request.getRemoteAddr();
            return "ip:" + (remoteAddr != null ? remoteAddr : "unknown");
        }

        return "system";
    }
}
