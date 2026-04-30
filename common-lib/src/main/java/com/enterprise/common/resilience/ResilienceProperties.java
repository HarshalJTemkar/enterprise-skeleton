package com.enterprise.common.resilience;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for the platform's shared resilience defaults.
 *
 * <p>These values feed a programmatically created {@code "default"}
 * {@link io.github.resilience4j.circuitbreaker.CircuitBreaker},
 * {@link io.github.resilience4j.ratelimiter.RateLimiter},
 * {@link io.github.resilience4j.timelimiter.TimeLimiter},
 * {@link io.github.resilience4j.retry.Retry} and
 * {@link io.github.resilience4j.bulkhead.Bulkhead} instance, so services can
 * start annotating methods with
 * {@code @CircuitBreaker(name = "default") / @RateLimiter(name = "default")}
 * etc. without any extra YAML. You can still override per-instance via the
 * native {@code resilience4j.*} property tree.</p>
 */
@ConfigurationProperties(prefix = "enterprise.common.resilience")
public class ResilienceProperties {

    /** Master switch for all resilience auto-configuration. */
    private boolean enabled = true;

    private final CircuitBreaker circuitBreaker = new CircuitBreaker();
    private final RateLimiter rateLimiter = new RateLimiter();
    private final TimeLimiter timeLimiter = new TimeLimiter();
    private final Retry retry = new Retry();
    private final Bulkhead bulkhead = new Bulkhead();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public CircuitBreaker getCircuitBreaker() { return circuitBreaker; }
    public RateLimiter getRateLimiter() { return rateLimiter; }
    public TimeLimiter getTimeLimiter() { return timeLimiter; }
    public Retry getRetry() { return retry; }
    public Bulkhead getBulkhead() { return bulkhead; }

    /** Properties backing the "default" CircuitBreaker instance. */
    public static class CircuitBreaker {
        /** Individual feature enable flag. */
        private boolean enabled = true;
        /** Failure rate (%) above which the breaker opens. */
        private float failureRateThreshold = 50f;
        /** Slow-call rate (%) above which the breaker opens. */
        private float slowCallRateThreshold = 100f;
        /** Duration above which a call is considered "slow". */
        private Duration slowCallDurationThreshold = Duration.ofSeconds(2);
        /** Time spent in OPEN state before transitioning to HALF_OPEN. */
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);
        /** Number of permitted test calls while HALF_OPEN. */
        private int permittedCallsInHalfOpenState = 5;
        /** Sliding window size (count-based). */
        private int slidingWindowSize = 20;
        /** Minimum calls before the breaker can calculate rates. */
        private int minimumNumberOfCalls = 10;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public float getFailureRateThreshold() { return failureRateThreshold; }
        public void setFailureRateThreshold(float v) { this.failureRateThreshold = v; }
        public float getSlowCallRateThreshold() { return slowCallRateThreshold; }
        public void setSlowCallRateThreshold(float v) { this.slowCallRateThreshold = v; }
        public Duration getSlowCallDurationThreshold() { return slowCallDurationThreshold; }
        public void setSlowCallDurationThreshold(Duration v) { this.slowCallDurationThreshold = v; }
        public Duration getWaitDurationInOpenState() { return waitDurationInOpenState; }
        public void setWaitDurationInOpenState(Duration v) { this.waitDurationInOpenState = v; }
        public int getPermittedCallsInHalfOpenState() { return permittedCallsInHalfOpenState; }
        public void setPermittedCallsInHalfOpenState(int v) { this.permittedCallsInHalfOpenState = v; }
        public int getSlidingWindowSize() { return slidingWindowSize; }
        public void setSlidingWindowSize(int v) { this.slidingWindowSize = v; }
        public int getMinimumNumberOfCalls() { return minimumNumberOfCalls; }
        public void setMinimumNumberOfCalls(int v) { this.minimumNumberOfCalls = v; }
    }

    /** Properties backing the "default" RateLimiter instance. */
    public static class RateLimiter {
        private boolean enabled = true;
        /** Max concurrent permits issued per refresh period. */
        private int limitForPeriod = 100;
        /** Refresh window length. */
        private Duration limitRefreshPeriod = Duration.ofSeconds(1);
        /** Max time a caller waits for a permit. */
        private Duration timeoutDuration = Duration.ofMillis(500);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getLimitForPeriod() { return limitForPeriod; }
        public void setLimitForPeriod(int v) { this.limitForPeriod = v; }
        public Duration getLimitRefreshPeriod() { return limitRefreshPeriod; }
        public void setLimitRefreshPeriod(Duration v) { this.limitRefreshPeriod = v; }
        public Duration getTimeoutDuration() { return timeoutDuration; }
        public void setTimeoutDuration(Duration v) { this.timeoutDuration = v; }
    }

    /** Properties backing the "default" TimeLimiter instance. */
    public static class TimeLimiter {
        private boolean enabled = true;
        /** Maximum wall-clock time for an async call. */
        private Duration timeoutDuration = Duration.ofSeconds(3);
        /** Whether to cancel the running {@code Future} on timeout. */
        private boolean cancelRunningFuture = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Duration getTimeoutDuration() { return timeoutDuration; }
        public void setTimeoutDuration(Duration v) { this.timeoutDuration = v; }
        public boolean isCancelRunningFuture() { return cancelRunningFuture; }
        public void setCancelRunningFuture(boolean v) { this.cancelRunningFuture = v; }
    }

    /** Properties backing the "default" Retry instance. */
    public static class Retry {
        private boolean enabled = true;
        /** Total attempts including the first call. */
        private int maxAttempts = 3;
        /** Backoff between attempts. */
        private Duration waitDuration = Duration.ofMillis(500);
        /** Exponential multiplier applied to {@link #waitDuration}. 1.0 = flat. */
        private double exponentialBackoffMultiplier = 2.0d;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int v) { this.maxAttempts = v; }
        public Duration getWaitDuration() { return waitDuration; }
        public void setWaitDuration(Duration v) { this.waitDuration = v; }
        public double getExponentialBackoffMultiplier() { return exponentialBackoffMultiplier; }
        public void setExponentialBackoffMultiplier(double v) { this.exponentialBackoffMultiplier = v; }
    }

    /** Properties backing the "default" (semaphore) Bulkhead instance. */
    public static class Bulkhead {
        private boolean enabled = true;
        /** Max concurrent calls allowed through. */
        private int maxConcurrentCalls = 25;
        /** Max time a caller waits to enter the bulkhead. */
        private Duration maxWaitDuration = Duration.ofMillis(100);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxConcurrentCalls() { return maxConcurrentCalls; }
        public void setMaxConcurrentCalls(int v) { this.maxConcurrentCalls = v; }
        public Duration getMaxWaitDuration() { return maxWaitDuration; }
        public void setMaxWaitDuration(Duration v) { this.maxWaitDuration = v; }
    }
}
