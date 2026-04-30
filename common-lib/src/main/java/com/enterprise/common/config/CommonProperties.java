package com.enterprise.common.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * All common-lib features are driven by these properties. Everything is configurable.
 */
@ConfigurationProperties(prefix = "enterprise.common")
public record CommonProperties(
        Web web,
        Security security,
        Jwt jwt) {

    public CommonProperties {
        if (web == null) web = new Web(new Web.ExceptionHandler(true), new Web.Cors(false, "*", "*", "*"));
        if (security == null) security = new Security(false);
        if (jwt == null) jwt = new Jwt("change-me-change-me-change-me-change-me-1234", Duration.ofHours(1),
                Duration.ofDays(7), "enterprise-platform", "Authorization", "Bearer ");
    }

    public record Web(ExceptionHandler exceptionHandler, Cors cors) {
        public record ExceptionHandler(boolean enabled) {}
        public record Cors(boolean enabled, String allowedOrigins, String allowedMethods, String allowedHeaders) {}
    }

    public record Security(boolean enabled) {}

    public record Jwt(
            String secret,
            Duration accessTokenTtl,
            Duration refreshTokenTtl,
            String issuer,
            String headerName,
            String tokenPrefix) {}
}
