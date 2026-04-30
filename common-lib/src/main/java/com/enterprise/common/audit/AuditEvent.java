package com.enterprise.common.audit;

import com.enterprise.common.enums.AuditAction;
import java.time.Instant;

/**
 * Immutable audit record emitted for every {@link Auditable} invocation.
 *
 * @param who           principal performing the action
 * @param action        verb (CREATE / UPDATE / DELETE …)
 * @param resource      logical resource name
 * @param resourceId    identifier of the affected resource (if known)
 * @param when          time of the action (UTC)
 * @param correlationId SLF4J MDC correlation id, if any
 * @param before        serialized state before the call (may be {@code null})
 * @param after         serialized state after the call (may be {@code null})
 * @param success       {@code true} if the method returned normally
 * @param errorMessage  exception message if {@link #success} is {@code false}
 */
public record AuditEvent(
        String who,
        AuditAction action,
        String resource,
        String resourceId,
        Instant when,
        String correlationId,
        String before,
        String after,
        boolean success,
        String errorMessage) {}
