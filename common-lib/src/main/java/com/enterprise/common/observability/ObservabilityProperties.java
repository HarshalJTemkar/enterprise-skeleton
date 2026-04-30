package com.enterprise.common.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code enterprise.common.observability.*}.
 *
 * <p>Tracing-related toggles. The Spring Boot 3 starter for
 * {@code micrometer-tracing-bridge-otel} reads
 * {@code management.tracing.*} natively; we add a thin layer on top so the
 * platform can flip the whole stack with one switch and so the OTLP endpoint
 * surfaces in {@code /actuator/info}.</p>
 */
@ConfigurationProperties(prefix = "enterprise.common.observability")
public class ObservabilityProperties {

    private final Tracing tracing = new Tracing();

    public Tracing getTracing() { return tracing; }

    /** Tracing settings forwarded to Micrometer / OpenTelemetry. */
    public static class Tracing {

        /** Master switch for tracing. */
        private boolean enabled = false;

        /** OTLP collector endpoint, e.g. {@code http://otel-collector:4318/v1/traces}. */
        private String endpoint = "http://localhost:4318/v1/traces";

        /** Sampling probability 0.0 – 1.0 (1.0 = sample every request). */
        private double samplingProbability = 1.0d;

        /** Service name reported as the resource attribute. */
        private String serviceName = "enterprise-service";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String v) { this.endpoint = v; }
        public double getSamplingProbability() { return samplingProbability; }
        public void setSamplingProbability(double v) { this.samplingProbability = v; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String v) { this.serviceName = v; }
    }
}
