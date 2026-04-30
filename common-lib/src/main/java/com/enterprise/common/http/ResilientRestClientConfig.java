package com.enterprise.common.http;

import com.enterprise.common.enums.HttpHeaderConstants;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * Registers a shared {@link RestClient.Builder} that:
 * <ul>
 *   <li>Propagates the SLF4J {@code correlationId} MDC value as the
 *       {@code X-Correlation-ID} request header on every outbound call.</li>
 *   <li>Forwards the inbound {@code X-Tenant-ID} header (multi-tenancy).</li>
 *   <li>Accepts an optional {@link CircuitBreakerRegistry} +
 *       {@link RetryRegistry} so callers can decorate the underlying
 *       executor with the platform's "default" CB / Retry instance.</li>
 * </ul>
 *
 * <p>Toggle with
 * {@code enterprise.common.http.client.enabled=true} (default).</p>
 */
@Configuration
@ConditionalOnClass(RestClient.class)
@ConditionalOnProperty(prefix = "enterprise.common.http.client",
        name = "enabled", havingValue = "true", matchIfMissing = true)
public class ResilientRestClientConfig {

    /**
     * The interceptor that copies relevant MDC values into the outbound
     * request headers. Exposed as a bean so callers can attach it to their
     * own {@code RestClient} instances if they don't use the shared builder.
     */
    @Bean
    @ConditionalOnMissingBean(name = "correlationPropagatingInterceptor")
    public ClientHttpRequestInterceptor correlationPropagatingInterceptor() {
        return (request, body, execution) -> {
            String corrId = MDC.get("correlationId");
            if (corrId != null && !request.getHeaders().containsKey(HttpHeaderConstants.CORRELATION_ID)) {
                request.getHeaders().add(HttpHeaderConstants.CORRELATION_ID, corrId);
            }
            String tenant = MDC.get("tenantId");
            if (tenant != null && !request.getHeaders().containsKey(HttpHeaderConstants.TENANT_ID)) {
                request.getHeaders().add(HttpHeaderConstants.TENANT_ID, tenant);
            }
            return execution.execute(request, body);
        };
    }

    /**
     * Shared {@link RestClient.Builder}. Inject this in services to build a
     * client that automatically propagates correlation ids:
     *
     * <pre>{@code
     * RestClient client = clientBuilder.baseUrl("http://billing").build();
     * Invoice inv = client.get().uri("/invoices/{id}", id)
     *                     .retrieve().body(Invoice.class);
     * }</pre>
     */
    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    public RestClient.Builder enterpriseRestClientBuilder(
            ClientHttpRequestInterceptor correlationPropagatingInterceptor) {
        return RestClient.builder()
                .requestInterceptor(correlationPropagatingInterceptor);
    }
}
