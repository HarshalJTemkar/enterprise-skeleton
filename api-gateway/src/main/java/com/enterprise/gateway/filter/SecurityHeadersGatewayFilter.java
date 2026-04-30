package com.enterprise.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Adds security headers to all responses for defense-in-depth.
 *
 * <p>Headers added:
 *
 * <ul>
 *   <li>X-Content-Type-Options: nosniff
 *   <li>X-Frame-Options: DENY
 *   <li>X-XSS-Protection: 1; mode=block
 *   <li>Strict-Transport-Security: max-age=31536000; includeSubDomains (HTTPS only)
 *   <li>Content-Security-Policy: default-src 'self'
 *   <li>Referrer-Policy: strict-origin-when-cross-origin
 *   <li>Permissions-Policy: geolocation=(), camera=(), microphone=()
 * </ul>
 */
@Slf4j
@Component
public class SecurityHeadersGatewayFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return chain
        .filter(exchange)
        .then(
            Mono.fromRunnable(
                () -> {
                  HttpHeaders headers = exchange.getResponse().getHeaders();

                  // Prevent MIME-type sniffing
                  headers.add("X-Content-Type-Options", "nosniff");

                  // Prevent clickjacking
                  headers.add("X-Frame-Options", "DENY");

                  // Enable XSS protection (legacy browsers)
                  headers.add("X-XSS-Protection", "1; mode=block");

                  // HSTS for HTTPS (only add if request is HTTPS)
                  if ("https".equalsIgnoreCase(exchange.getRequest().getURI().getScheme())) {
                    headers.add(
                        "Strict-Transport-Security",
                        "max-age=31536000; includeSubDomains; preload");
                  }

                  // Content Security Policy
                  headers.add(
                      "Content-Security-Policy",
                      "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'");

                  // Referrer policy
                  headers.add("Referrer-Policy", "strict-origin-when-cross-origin");

                  // Permissions policy (feature policy)
                  headers.add(
                      "Permissions-Policy", "geolocation=(), camera=(), microphone=(), payment=()");

                  // Remove sensitive server headers
                  headers.remove("Server");
                  headers.remove("X-Powered-By");

                  log.trace(
                      "Added security headers to response for path: {}",
                      exchange.getRequest().getURI().getPath());
                }));
  }

  @Override
  public int getOrder() {
    // Run late, after other filters have processed
    return Ordered.LOWEST_PRECEDENCE;
  }
}
