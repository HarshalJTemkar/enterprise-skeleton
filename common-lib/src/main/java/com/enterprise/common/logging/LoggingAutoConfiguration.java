package com.enterprise.common.logging;

import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the correlation-id filter, the payload masker and the optional
 * access-log filter. Every filter is independently toggleable:
 *
 * <pre>
 * enterprise.common.logging.correlation-id.enabled=true
 * enterprise.common.logging.access-log.enabled=true
 * enterprise.common.logging.masking.enabled=true
 * </pre>
 */
@Configuration
@ConditionalOnClass(Filter.class)
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingAutoConfiguration {

    /** Shared masker bean, used by the access log filter and any custom loggers. */
    @Bean
    public PayloadMasker payloadMasker(LoggingProperties props) {
        return new PayloadMasker(props.getMasking());
    }

    /** Correlation-id filter (always first). */
    @Bean
    @ConditionalOnProperty(prefix = "enterprise.common.logging.correlation-id",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter(LoggingProperties props) {
        FilterRegistrationBean<CorrelationIdFilter> reg = new FilterRegistrationBean<>(
                new CorrelationIdFilter(props.getCorrelationId()));
        reg.setOrder(Integer.MIN_VALUE + 10);
        reg.addUrlPatterns("/*");
        return reg;
    }

    /** Access-log filter (toggleable; runs after correlation-id filter). */
    @Bean
    @ConditionalOnProperty(prefix = "enterprise.common.logging.access-log",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<RequestResponseLoggingFilter> accessLogFilter(
            LoggingProperties props, PayloadMasker masker) {
        FilterRegistrationBean<RequestResponseLoggingFilter> reg = new FilterRegistrationBean<>(
                new RequestResponseLoggingFilter(props.getAccessLog(), masker));
        reg.setOrder(Integer.MIN_VALUE + 20);
        reg.addUrlPatterns("/*");
        return reg;
    }
}
