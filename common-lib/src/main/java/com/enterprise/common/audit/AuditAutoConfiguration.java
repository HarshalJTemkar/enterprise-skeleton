package com.enterprise.common.audit;

import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the {@link AuditAspect} and a default SLF4J-based {@link AuditEventPublisher}. Disable
 * entirely with {@code feature.audit.enabled=false}.
 */
@Configuration
@ConditionalOnClass(Aspect.class)
@ConditionalOnProperty(
    prefix = "feature.audit",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AuditAutoConfiguration {

  /** Default audit sink – writes a structured line on the {@code AUDIT} logger. */
  @Bean
  @ConditionalOnMissingBean
  public AuditEventPublisher auditEventPublisher() {
    Logger audit = LoggerFactory.getLogger("AUDIT");
    return event ->
        audit.info(
            "audit action={} resource={} id={} who={} success={} corrId={} when={} error=\"{}\"",
            event.action(),
            event.resource(),
            event.resourceId(),
            event.who(),
            event.success(),
            event.correlationId(),
            event.when(),
            event.errorMessage() == null ? "" : event.errorMessage());
  }

  /** The AOP aspect applied to every {@code @Auditable} method. */
  @Bean
  @ConditionalOnMissingBean
  public AuditAspect auditAspect(AuditEventPublisher publisher) {
    return new AuditAspect(publisher);
  }
}
