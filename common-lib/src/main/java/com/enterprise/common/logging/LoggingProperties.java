package com.enterprise.common.logging;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code enterprise.common.logging.*}. Drives the correlation-id filter
 * and the request/response logging filter including body masking.
 */
@ConfigurationProperties(prefix = "enterprise.common.logging")
public class LoggingProperties {

    private final CorrelationId correlationId = new CorrelationId();
    private final AccessLog accessLog = new AccessLog();
    private final Masking masking = new Masking();

    public CorrelationId getCorrelationId() { return correlationId; }
    public AccessLog getAccessLog() { return accessLog; }
    public Masking getMasking() { return masking; }

    /** Correlation-ID filter settings. */
    public static class CorrelationId {
        /** Disable to skip registering the filter entirely. */
        private boolean enabled = true;
        /** Header to read (and echo back) the correlation id on. */
        private String headerName = "X-Correlation-ID";
        /** Key under which the id is stored in the SLF4J {@code MDC}. */
        private String mdcKey = "correlationId";
        /** Also generate an {@code X-Request-ID} header per hop. */
        private boolean generateRequestId = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public String getHeaderName() { return headerName; }
        public void setHeaderName(String v) { this.headerName = v; }
        public String getMdcKey() { return mdcKey; }
        public void setMdcKey(String v) { this.mdcKey = v; }
        public boolean isGenerateRequestId() { return generateRequestId; }
        public void setGenerateRequestId(boolean v) { this.generateRequestId = v; }
    }

    /** Request/response access-log filter settings. */
    public static class AccessLog {
        /** Enable the filter. */
        private boolean enabled = true;
        /** Include request / response bodies in the log line. */
        private boolean includePayload = true;
        /** Max number of body bytes captured for logging. */
        private int maxPayloadLength = 2048;
        /** URI patterns that are never logged (health checks etc.). */
        private List<String> excludePatterns = List.of(
                "/actuator/**", "/favicon.ico", "/v3/api-docs/**", "/swagger-ui/**");

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public boolean isIncludePayload() { return includePayload; }
        public void setIncludePayload(boolean v) { this.includePayload = v; }
        public int getMaxPayloadLength() { return maxPayloadLength; }
        public void setMaxPayloadLength(int v) { this.maxPayloadLength = v; }
        public List<String> getExcludePatterns() { return excludePatterns; }
        public void setExcludePatterns(List<String> v) { this.excludePatterns = v; }
    }

    /** PII / secret masking rules applied to payloads and MDC values. */
    public static class Masking {
        /** Enable masking. */
        private boolean enabled = true;
        /**
         * JSON field names whose <em>values</em> are replaced with {@code ***}
         * in logs (case-insensitive).
         */
        private List<String> fields = List.of(
                "password", "passwd", "secret", "token", "access_token",
                "refresh_token", "authorization", "apiKey", "api_key",
                "ssn", "cardNumber", "card_number", "cvv", "pin");
        /** Replacement token. */
        private String mask = "***";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public List<String> getFields() { return fields; }
        public void setFields(List<String> v) { this.fields = v; }
        public String getMask() { return mask; }
        public void setMask(String v) { this.mask = v; }
    }
}
