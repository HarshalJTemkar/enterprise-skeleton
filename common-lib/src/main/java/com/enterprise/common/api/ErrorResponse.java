package com.enterprise.common.api;

import com.enterprise.common.exception.ValidationException.FieldViolation;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Structured error envelope returned by {@code GlobalExceptionHandler}. Also surfaces inside {@link
 * org.springframework.http.ProblemDetail} as the {@code "error"} member.
 */
public record ErrorResponse(
    String errorCode,
    String message,
    String correlationId,
    Instant timestamp,
    String path,
    List<FieldViolation> errors,
    Map<String, Object> details) {}
