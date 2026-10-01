package com.passport.common.ratelimit;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a client exhausts their token bucket.
 */
@Getter
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {

    private final RateLimitResult result;

    public RateLimitExceededException(String message, RateLimitResult result) {
        super(message);
        this.result = result;
    }
}
