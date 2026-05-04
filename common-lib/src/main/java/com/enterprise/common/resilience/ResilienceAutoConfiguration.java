package com.enterprise.common.resilience;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatically builds one {@code "default"} instance per Resilience4j policy, seeded from
 * {@link ResilienceProperties}. Services can either:
 *
 * <ul>
 *   <li>Annotate methods with {@code @CircuitBreaker(name="default")},
 *       {@code @RateLimiter(name="default")}, {@code @TimeLimiter(name="default")},
 *       {@code @Retry(name="default")}, {@code @Bulkhead(name="default")}; or
 *   <li>Inject the registry directly to create ad-hoc named instances.
 * </ul>
 *
 * <p>Every sub-feature can be disabled independently, e.g.:
 *
 * <pre>
 * enterprise.common.resilience.rate-limiter.enabled=false
 * </pre>
 */
@Configuration
@ConditionalOnClass(CircuitBreakerRegistry.class)
@ConditionalOnProperty(
    prefix = "enterprise.common.resilience",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(ResilienceProperties.class)
public class ResilienceAutoConfiguration {

  /** Name used for the platform-wide default instance of every policy. */
  public static final String DEFAULT = "default";

  /**
   * Builds a {@link CircuitBreakerRegistry} containing a single {@code "default"} circuit breaker.
   * The resilience4j-spring-boot3 starter usually provides one itself from YAML, so this bean is
   * only created when the application has not already declared one.
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "enterprise.common.resilience.circuit-breaker",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public CircuitBreakerRegistry circuitBreakerRegistry(ResilienceProperties props) {
    var p = props.getCircuitBreaker();
    CircuitBreakerConfig cfg =
        CircuitBreakerConfig.custom()
            .failureRateThreshold(p.getFailureRateThreshold())
            .slowCallRateThreshold(p.getSlowCallRateThreshold())
            .slowCallDurationThreshold(p.getSlowCallDurationThreshold())
            .waitDurationInOpenState(p.getWaitDurationInOpenState())
            .permittedNumberOfCallsInHalfOpenState(p.getPermittedCallsInHalfOpenState())
            .slidingWindowSize(p.getSlidingWindowSize())
            .minimumNumberOfCalls(p.getMinimumNumberOfCalls())
            .build();
    CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(cfg);
    registry.circuitBreaker(DEFAULT);
    return registry;
  }

  /**
   * Builds a {@link RateLimiterRegistry} with a single {@code "default"} rate limiter: {@code
   * limitForPeriod} permits issued every {@code limitRefreshPeriod}, waiting up to {@code
   * timeoutDuration}.
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "enterprise.common.resilience.rate-limiter",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public RateLimiterRegistry rateLimiterRegistry(ResilienceProperties props) {
    var p = props.getRateLimiter();
    RateLimiterConfig cfg =
        RateLimiterConfig.custom()
            .limitForPeriod(p.getLimitForPeriod())
            .limitRefreshPeriod(p.getLimitRefreshPeriod())
            .timeoutDuration(p.getTimeoutDuration())
            .build();
    RateLimiterRegistry registry = RateLimiterRegistry.of(cfg);
    registry.rateLimiter(DEFAULT);
    return registry;
  }

  /**
   * Builds a {@link TimeLimiterRegistry} with a single {@code "default"} time limiter that wraps
   * {@link java.util.concurrent.CompletionStage} calls with a timeout.
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "enterprise.common.resilience.time-limiter",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public TimeLimiterRegistry timeLimiterRegistry(ResilienceProperties props) {
    var p = props.getTimeLimiter();
    TimeLimiterConfig cfg =
        TimeLimiterConfig.custom()
            .timeoutDuration(p.getTimeoutDuration())
            .cancelRunningFuture(p.isCancelRunningFuture())
            .build();
    TimeLimiterRegistry registry = TimeLimiterRegistry.of(cfg);
    registry.timeLimiter(DEFAULT);
    return registry;
  }

  /**
   * Builds a {@link RetryRegistry} with exponential backoff based on {@link
   * ResilienceProperties.Retry}.
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "enterprise.common.resilience.retry",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public RetryRegistry retryRegistry(ResilienceProperties props) {
    var p = props.getRetry();
    RetryConfig.Builder<Object> b = RetryConfig.<Object>custom().maxAttempts(p.getMaxAttempts());
    // RetryConfig forbids setting both waitDuration and intervalFunction.
    // Use exponential backoff when multiplier > 1, else a fixed waitDuration.
    if (p.getExponentialBackoffMultiplier() > 1.0d) {
      b.intervalFunction(
          io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(
              p.getWaitDuration().toMillis(), p.getExponentialBackoffMultiplier()));
    } else {
      b.waitDuration(p.getWaitDuration());
    }
    RetryRegistry registry = RetryRegistry.of(b.build());
    registry.retry(DEFAULT);
    return registry;
  }

  /**
   * Builds a {@link BulkheadRegistry} backed by a semaphore bulkhead to cap concurrent callers
   * through a protected section of code.
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "enterprise.common.resilience.bulkhead",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public BulkheadRegistry bulkheadRegistry(ResilienceProperties props) {
    var p = props.getBulkhead();
    BulkheadConfig cfg =
        BulkheadConfig.custom()
            .maxConcurrentCalls(p.getMaxConcurrentCalls())
            .maxWaitDuration(p.getMaxWaitDuration())
            .build();
    BulkheadRegistry registry = BulkheadRegistry.of(cfg);
    registry.bulkhead(DEFAULT);
    return registry;
  }
}
