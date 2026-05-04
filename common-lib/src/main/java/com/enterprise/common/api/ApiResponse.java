package com.enterprise.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import org.slf4j.MDC;

/**
 * Consistent success envelope returned by REST endpoints.
 *
 * <p>Shape:
 *
 * <pre>{@code
 * { "status": "success",
 *   "data": { ... },
 *   "message": "optional",
 *   "correlationId": "abc-123",
 *   "timestamp": "2026-01-01T00:00:00Z" }
 * }</pre>
 *
 * <p>Failures are <em>not</em> wrapped in {@code ApiResponse} — they are emitted as RFC 7807 {@link
 * org.springframework.http.ProblemDetail} documents with an {@link ErrorResponse} envelope by
 * {@code GlobalExceptionHandler}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    String status, T data, String message, String correlationId, Instant timestamp) {

  /** Build a success envelope carrying {@code data} only. */
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>("success", data, null, MDC.get("correlationId"), Instant.now());
  }

  /** Build a success envelope with a human-readable message. */
  public static <T> ApiResponse<T> ok(T data, String message) {
    return new ApiResponse<>("success", data, message, MDC.get("correlationId"), Instant.now());
  }

  /** Convenience for endpoints that only need to return a message. */
  public static ApiResponse<Void> message(String message) {
    return new ApiResponse<>("success", null, message, MDC.get("correlationId"), Instant.now());
  }
}
