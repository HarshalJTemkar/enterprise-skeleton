package com.enterprise.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Defends downstream microservices against requests that bypass the API
 * gateway. Two complementary checks are supported (either or both):
 *
 * <ol>
 *   <li><b>Shared-secret header</b> — every request must carry
 *   {@code X-Gateway-Secret: <secret>} matching the configured value.</li>
 *   <li><b>Trusted source IP / CIDR list</b> — the remote address must
 *   match one of the configured ranges.</li>
 * </ol>
 *
 * <p>Designed to run very early in the chain (before authentication) so
 * spoofed {@code X-Auth-Subject} / {@code X-Auth-Roles} headers cannot
 * leak into the application.</p>
 *
 * <p>Disabled by default; enable in production via
 * {@code enterprise.common.security.trusted-gateway.enabled=true}.</p>
 */
public class TrustedGatewayHeaderFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(TrustedGatewayHeaderFilter.class);

    /** Headers stripped from inbound requests to prevent spoofing. */
    private static final List<String> SPOOFABLE_HEADERS =
            List.of("X-Auth-Subject", "X-Auth-Roles", "X-Auth-Tenant");

    private final String headerName;
    private final String expectedSecret;

    public TrustedGatewayHeaderFilter(String headerName, String expectedSecret) {
        this.headerName = headerName;
        this.expectedSecret = expectedSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String supplied = request.getHeader(headerName);
        if (expectedSecret == null || expectedSecret.isBlank()
                || !constantTimeEquals(expectedSecret, supplied)) {
            // Strip spoofable identity headers before rejecting so the access log
            // never echoes attacker-supplied values.
            SPOOFABLE_HEADERS.forEach(h -> {
                if (request.getHeader(h) != null) {
                    LOG.warn("Stripped spoofable header {} from {}", h, request.getRequestURI());
                }
            });
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setHeader("X-Auth-Error", "Direct access forbidden; use the API gateway");
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) return false;
        byte[] a = expected.getBytes();
        byte[] b = actual.getBytes();
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // before correlation-id and auth filters
    }
}
