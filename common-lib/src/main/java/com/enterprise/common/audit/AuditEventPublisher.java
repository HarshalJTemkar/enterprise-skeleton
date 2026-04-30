package com.enterprise.common.audit;

/**
 * Sink for {@link AuditEvent}s produced by {@link AuditAspect}. The default
 * implementation logs to SLF4J (category {@code AUDIT}); replace with a
 * custom bean to ship to a database, Kafka, SIEM, etc.
 */
@FunctionalInterface
public interface AuditEventPublisher {
    /**
     * Handle an audit event. Implementations must be non-blocking / fail-safe
     * so audit failures never mask the business operation's outcome.
     */
    void publish(AuditEvent event);
}
