package com.enterprise.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Provides the {@code KeyResolver} consumed by the Redis-backed {@code RequestRateLimiter} filter.
 *
 * <p>The default strategy keys by the remote IP. Switch to a header (e.g. {@code X-Auth-Subject})
 * by changing the SpEL expression in {@code application.yml} to {@code "#{@principalKeyResolver}"}
 * and exposing a corresponding bean.
 */
@Configuration
public class RateLimiterConfig {

  /** Key resolver: rate-limit per client IP. */
  @Bean(name = "ipKeyResolver")
  @Primary
  public KeyResolver ipKeyResolver() {
    return exchange -> {
      var address = exchange.getRequest().getRemoteAddress();
      String ip = address == null ? "unknown" : address.getAddress().getHostAddress();
      String fwd = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
      if (fwd != null && !fwd.isBlank()) ip = fwd.split(",")[0].trim();
      return Mono.just(ip);
    };
  }

  /** Optional alternative: rate-limit per authenticated subject. */
  @Bean(name = "principalKeyResolver")
  public KeyResolver principalKeyResolver() {
    return exchange -> {
      String subject = exchange.getRequest().getHeaders().getFirst("X-Auth-Subject");
      return Mono.just(subject == null ? "anonymous" : subject);
    };
  }
}
