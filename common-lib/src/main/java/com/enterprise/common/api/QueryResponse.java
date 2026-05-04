package com.enterprise.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;

/**
 * Generic envelope for <b>GraphQL</b> query / mutation results.
 *
 * <p>Mirrors {@link ApiResponse} for REST, but is shaped to coexist with GraphQL's own {@code data}
 * / {@code errors} document. Resolvers should return {@code QueryResponse<T>} as the {@code data}
 * field of the GraphQL type so clients receive a uniform envelope across REST and GraphQL.
 *
 * <p>Example schema:
 *
 * <pre>{@code
 * type WhoAmIResponse {
 *   status: String!
 *   data: WhoAmI
 *   message: String
 *   correlationId: String
 *   timestamp: String!
 *   warnings: [String!]
 *   extensions: JSON
 * }
 * }</pre>
 *
 * <p>Failures are <em>not</em> wrapped — they are emitted as standard GraphQL errors (with {@code
 * extensions.errorCode} populated by {@code GraphQlExceptionResolver}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueryResponse<T>(
    String status,
    T data,
    String message,
    String correlationId,
    Instant timestamp,
    List<String> warnings,
    Map<String, Object> extensions) {

  /** Build a success envelope carrying {@code data} only. */
  public static <T> QueryResponse<T> ok(T data) {
    return new QueryResponse<>(
        "success", data, null, MDC.get("correlationId"), Instant.now(), List.of(), Map.of());
  }

  /** Build a success envelope with a human-readable message. */
  public static <T> QueryResponse<T> ok(T data, String message) {
    return new QueryResponse<>(
        "success", data, message, MDC.get("correlationId"), Instant.now(), List.of(), Map.of());
  }

  /** Build a success envelope with non-fatal warnings (e.g. partial results). */
  public static <T> QueryResponse<T> okWithWarnings(T data, List<String> warnings) {
    return new QueryResponse<>(
        "success",
        data,
        null,
        MDC.get("correlationId"),
        Instant.now(),
        warnings == null ? List.of() : warnings,
        Map.of());
  }

  /** Build a success envelope with extra metadata under {@code extensions}. */
  public static <T> QueryResponse<T> okWithExtensions(T data, Map<String, Object> extensions) {
    return new QueryResponse<>(
        "success",
        data,
        null,
        MDC.get("correlationId"),
        Instant.now(),
        List.of(),
        extensions == null ? Map.of() : extensions);
  }

  /** Convenience for resolvers that only need to acknowledge an action. */
  public static QueryResponse<Void> message(String message) {
    return new QueryResponse<>(
        "success", null, message, MDC.get("correlationId"), Instant.now(), List.of(), Map.of());
  }

  /** Convenience for an empty {@code null} result (e.g. an entity lookup miss). */
  public static <T> QueryResponse<T> empty() {
    return new QueryResponse<>(
        "success", null, null, MDC.get("correlationId"), Instant.now(), List.of(), Map.of());
  }
}
