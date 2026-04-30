package com.enterprise.common.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * Test annotation that populates the {@link org.springframework.security.core.context.SecurityContext}
 * with a synthetic JWT-style authentication. Equivalent to
 * {@code @WithMockUser} but exposes an additional {@link #subject()} field
 * that maps to the JWT {@code sub} claim.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * @WithMockJwtUser(username = "alice", roles = {"USER", "ADMIN"})
 * @Test
 * void shouldDoX() { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithMockJwtUserSecurityContextFactory.class)
public @interface WithMockJwtUser {

    /** JWT subject / Spring principal name. */
    String username() default "test-user";

    /** Optional explicit subject; defaults to {@link #username()}. */
    String subject() default "";

    /** Roles granted (without the {@code ROLE_} prefix). */
    String[] roles() default {"USER"};
}
