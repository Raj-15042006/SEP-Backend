package com.passport.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enforce Token Bucket rate limiting on specific controller endpoints or service methods.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    /**
     * Maximum burst capacity of tokens in the bucket.
     */
    int capacity() default 60;

    /**
     * Tokens regenerated per second.
     */
    double refillRate() default 1.0;

    /**
     * Optional custom key prefix for bucket partitioning.
     */
    String keyPrefix() default "endpoint";

    /**
     * Number of tokens consumed per invocation.
     */
    int tokens() default 1;
}
