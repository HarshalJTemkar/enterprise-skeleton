package com.enterprise.common.audit;

import com.enterprise.common.enums.AuditAction;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method as audit-worthy. An AOP aspect captures the call
 * before/after the invocation, builds an {@link AuditEvent}, and forwards it
 * to the {@link AuditEventPublisher}.
 *
 * <p>Example:</p>
 * <pre>{@code
 * @Auditable(action = AuditAction.UPDATE, resource = "user")
 * public UserDto updateUser(Long id, UpdateUserRequest req) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Verb of the audited operation. */
    AuditAction action();

    /** Logical resource name (e.g. {@code "user"}, {@code "invoice"}). */
    String resource();

    /** Optional SpEL expression evaluated against the method args to produce the resource id. */
    String idExpression() default "";

    /** When {@code true}, the return value is serialized and stored as the "after" state. */
    boolean captureResult() default true;

    /** When {@code true}, the method arguments are serialized as the "before"/"input" state. */
    boolean captureArgs() default true;
}
