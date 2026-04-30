package com.enterprise.common.exception;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps Resilience4j runtime exceptions to RFC 7807 responses. Registered
 * only when {@code resilience4j-circuitbreaker} is on the classpath
 * (see {@code ResilienceExceptionAdviceAutoConfiguration}).
 *
 * <p>Mappings:</p>
 * <ul>
 *   <li>{@link CallNotPermittedException} → {@code 503 CIRCUIT_OPEN}</li>
 *   <li>{@link RequestNotPermitted}       → {@code 429 TOO_MANY_REQUESTS}</li>
 *   <li>{@link BulkheadFullException}     → {@code 503 BULKHEAD_FULL}</li>
 * </ul>
 */
@RestControllerAdvice
public class ResilienceExceptionAdvice {

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ProblemDetail> handleCircuitOpen(
            CallNotPermittedException ex, HttpServletRequest req) {
        return GlobalExceptionHandler.buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.CIRCUIT_OPEN,
                "Circuit breaker is open: " + ex.getMessage(),
                req, List.of(), Map.of("circuitBreaker", ex.getCausingCircuitBreakerName()));
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ProblemDetail> handleRateLimited(
            RequestNotPermitted ex, HttpServletRequest req) {
        return GlobalExceptionHandler.buildResponse(
                HttpStatus.TOO_MANY_REQUESTS, ErrorCode.TOO_MANY_REQUESTS,
                "Rate limit exceeded",
                req, List.of(), Map.of());
    }

    @ExceptionHandler(BulkheadFullException.class)
    public ResponseEntity<ProblemDetail> handleBulkheadFull(
            BulkheadFullException ex, HttpServletRequest req) {
        return GlobalExceptionHandler.buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.BULKHEAD_FULL,
                "Bulkhead full, capacity exceeded",
                req, List.of(), Map.of());
    }
}
