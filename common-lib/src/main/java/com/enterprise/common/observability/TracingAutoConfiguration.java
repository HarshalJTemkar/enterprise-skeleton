package com.enterprise.common.observability;

import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Tracing auto-configuration. The Spring Boot 3 micrometer-tracing starter already auto-configures
 * the {@link Tracer} when its dependencies are on the classpath; this class only:
 *
 * <ul>
 *   <li>Registers {@link ObservabilityProperties} so platform-level toggles are bindable.
 *   <li>Acts as a single, well-known activation point so the platform can enable/disable the whole
 *       stack with one property ({@code enterprise.common.observability.tracing.enabled=true}).
 * </ul>
 *
 * <p>OTLP endpoint, sampling and service-name are read by the upstream starter from {@code
 * management.tracing.*} and {@code management.otlp.tracing.*} (set in {@code config-server}
 * defaults).
 */
@Configuration
@ConditionalOnClass(Tracer.class)
@ConditionalOnProperty(
    prefix = "enterprise.common.observability.tracing",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
@EnableConfigurationProperties(ObservabilityProperties.class)
public class TracingAutoConfiguration {}
