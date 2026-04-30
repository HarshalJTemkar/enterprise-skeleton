package com.enterprise.common.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a method as idempotent. Combined with a {@code Idempotency-Key}
 * header, the first invocation's response is cached and any subsequent call
 * with the same key within the TTL replays the cached response instead of
 * re-executing the method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** Optional logical name used in the Redis key to avoid collisions. */
    String name() default "";

    /** Override the default TTL (ISO-8601 duration, e.g. {@code PT10M}). */
    String ttl() default "";
}
