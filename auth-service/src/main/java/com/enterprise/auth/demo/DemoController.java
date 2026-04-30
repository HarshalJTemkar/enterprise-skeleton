package com.enterprise.auth.demo;

import com.enterprise.common.api.ApiResponse;
import com.enterprise.common.async.ParallelExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Showcase endpoints that exercise every feature wired by {@code common-lib}:
 * Swagger, MapStruct, CircuitBreaker, RateLimiter, TimeLimiter, Retry,
 * Bulkhead, {@code @Async} and parallel fan-out via {@link ParallelExecutor}.
 */
@RestController
@RequestMapping("/demo")
@Tag(name = "Demo", description = "Feature showcase for common-lib capabilities")
public class DemoController {

    private final DemoResilienceService resilience;
    private final ParallelExecutor parallelExecutor;
    private final UserMapper userMapper;

    /**
     * Constructor injection. {@code ParallelExecutor} is only present when
     * {@code enterprise.common.async.parallel.enabled=true}, hence the
     * {@link Autowired}(required=false) style tolerated via Spring 6.
     */
    public DemoController(DemoResilienceService resilience,
                          ParallelExecutor parallelExecutor,
                          UserMapper userMapper) {
        this.resilience = resilience;
        this.parallelExecutor = parallelExecutor;
        this.userMapper = userMapper;
    }

    /** Triggers the CircuitBreaker-protected call. */
    @GetMapping("/circuit-breaker")
    @Operation(summary = "Invoke an unstable downstream guarded by a CircuitBreaker")
    public ApiResponse<String> circuitBreaker() {
        return ApiResponse.ok(resilience.unstableCall());
    }

    /** Triggers the RateLimiter. Call faster than the limit to see it reject. */
    @GetMapping("/rate-limiter")
    @Operation(summary = "Call a RateLimiter-protected operation")
    public ApiResponse<String> rateLimiter() {
        return ApiResponse.ok(resilience.rateLimitedCall());
    }

    /** Triggers a method that retries transient failures. */
    @GetMapping("/retry")
    @Operation(summary = "Call a flaky operation guarded by @Retry")
    public ApiResponse<String> retry() {
        return ApiResponse.ok(resilience.retryingCall());
    }

    /** Triggers a Bulkhead-protected call. */
    @GetMapping("/bulkhead")
    @Operation(summary = "Call a Bulkhead-protected operation")
    public ApiResponse<String> bulkhead() {
        return ApiResponse.ok(resilience.bulkheadedCall());
    }

    /**
     * Fires an async call that may exceed the TimeLimiter's budget. Pass
     * {@code ?delayMs=5000} to observe the timeout fallback.
     */
    @GetMapping("/async-timelimiter")
    @Operation(summary = "Async call wrapped with TimeLimiter + CircuitBreaker")
    public CompletableFuture<ApiResponse<String>> asyncTimeLimiter(
            @RequestParam(defaultValue = "500") long delayMs) {
        return resilience.slowAsyncCall(delayMs).thenApply(ApiResponse::ok);
    }

    /**
     * Runs several independent lookups <em>concurrently</em> on the parallel
     * executor and returns the aggregated result.
     */
    @GetMapping("/parallel")
    @Operation(summary = "Fan-out N tasks concurrently via ParallelExecutor")
    public ApiResponse<List<String>> parallel(@RequestParam(defaultValue = "5") int tasks) {
        List<Integer> inputs = java.util.stream.IntStream.rangeClosed(1, tasks).boxed().toList();
        List<String> results = parallelExecutor.map(inputs, i -> {
            try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            return "task-" + i + "@" + Thread.currentThread();
        });
        return ApiResponse.ok(results);
    }

    /**
     * Ordinary {@code @Async} method: the HTTP thread is released immediately
     * and the work continues on the shared {@code taskExecutor}.
     */
    @GetMapping("/fire-and-forget")
    @Operation(summary = "Submit work via @Async and return immediately")
    @Async
    public CompletableFuture<ApiResponse<String>> fireAndForget() {
        return CompletableFuture.completedFuture(
                ApiResponse.ok("submitted-on-" + Thread.currentThread()));
    }

    /**
     * Showcases the MapStruct mapper generated at compile time.
     */
    @GetMapping("/mapper")
    @Operation(summary = "Demonstrate the MapStruct entity → DTO mapping")
    public ApiResponse<UserDto> mapper() {
        UserEntity entity = new UserEntity(1L, "demo", "hashed-secret",
                List.of("ROLE_USER", "ROLE_ADMIN"));
        return ApiResponse.ok(userMapper.toDto(entity));
    }
}
