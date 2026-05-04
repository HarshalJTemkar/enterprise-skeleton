package com.enterprise.auth.demo;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Illustrates how every resilience / async feature from {@code common-lib} can be composed on a
 * single service class. Every method is annotated with Javadoc and a resilience behavior so the
 * integration is obvious.
 */
@Service
public class DemoResilienceService {

  private static final Logger log = LoggerFactory.getLogger(DemoResilienceService.class);
  private final AtomicInteger retryCounter = new AtomicInteger();

  /**
   * Synchronous call protected by a {@code CircuitBreaker}. When the downstream fails more than
   * {@code enterprise.common.resilience.circuit-breaker.failure-rate-threshold}% of the time, the
   * breaker opens and {@link #unstableFallback(Throwable)} is invoked instead.
   */
  @CircuitBreaker(name = "default", fallbackMethod = "unstableFallback")
  public String unstableCall() {
    if (ThreadLocalRandom.current().nextInt(100) < 40) {
      throw new IllegalStateException("Simulated downstream failure");
    }
    return "unstable-call-ok";
  }

  /** Fallback invoked by the circuit breaker. */
  public String unstableFallback(Throwable ex) {
    log.warn("CircuitBreaker fallback: {}", ex.getMessage());
    return "fallback";
  }

  /**
   * Rate-limited endpoint. The shared {@code "default"} rate limiter allows {@code
   * enterprise.common.resilience.rate-limiter.limit-for-period} calls per refresh period.
   */
  @RateLimiter(name = "default")
  public String rateLimitedCall() {
    return "rate-limit-ok-" + System.currentTimeMillis();
  }

  /**
   * Retries up to {@code max-attempts} times on failure with exponential backoff. Useful for
   * idempotent downstream calls.
   */
  @Retry(name = "default", fallbackMethod = "retryFallback")
  public String retryingCall() {
    int attempt = retryCounter.incrementAndGet();
    log.info("retryingCall attempt #{}", attempt);
    if (attempt % 3 != 0) {
      throw new IllegalStateException("Transient failure at attempt " + attempt);
    }
    retryCounter.set(0);
    return "retry-succeeded-on-attempt-" + attempt;
  }

  /** Retry fallback – invoked after attempts are exhausted. */
  public String retryFallback(Throwable ex) {
    retryCounter.set(0);
    return "retry-fallback: " + ex.getMessage();
  }

  /**
   * Demonstrates {@code TimeLimiter + CircuitBreaker + @Async} composition. The call returns a
   * {@link CompletableFuture} so Resilience4j's TimeLimiter can cancel it if it exceeds {@code
   * enterprise.common.resilience.time-limiter.timeout-duration}.
   */
  @TimeLimiter(name = "default", fallbackMethod = "slowFallback")
  @CircuitBreaker(name = "default")
  @Async
  public CompletableFuture<String> slowAsyncCall(long simulatedMs) {
    try {
      Thread.sleep(simulatedMs);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
    return CompletableFuture.completedFuture("slow-ok-after-" + simulatedMs + "ms");
  }

  /** Fallback used when {@link #slowAsyncCall(long)} times out. */
  public CompletableFuture<String> slowFallback(long simulatedMs, Throwable ex) {
    return CompletableFuture.completedFuture("timed-out: " + ex.getClass().getSimpleName());
  }

  /**
   * Semaphore-bounded section that caps concurrent callers to {@code
   * enterprise.common.resilience.bulkhead.max-concurrent-calls}.
   */
  @Bulkhead(name = "default")
  public String bulkheadedCall() {
    return "bulkhead-ok";
  }
}
