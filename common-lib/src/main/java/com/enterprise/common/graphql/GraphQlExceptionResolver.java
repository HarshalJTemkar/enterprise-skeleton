package com.enterprise.common.graphql;

import com.enterprise.common.exception.BaseException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.common.exception.ValidationException;
import com.enterprise.common.i18n.MessageService;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;

/**
 * Translates platform exceptions into GraphQL errors that carry the same
 * {@link ErrorCode} catalog used by REST. Every error appears under
 * {@code errors[].extensions} as:
 *
 * <pre>{@code
 * {
 *   "extensions": {
 *     "errorCode":      "ERR-1003",
 *     "classification": "INVALID_CREDENTIALS",
 *     "correlationId":  "abc-123",
 *     "timestamp":      "2026-04-29T12:00:00Z",
 *     "details":        { ... },
 *     "fieldErrors":    [ { "field":"email", "message":"must be email" } ]
 *   }
 * }
 * }</pre>
 *
 * <p>Mirrors {@code GlobalExceptionHandler} on the REST side so a client
 * sees the same code regardless of transport.</p>
 */
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    private static final Logger log = LoggerFactory.getLogger(GraphQlExceptionResolver.class);

    private final MessageService messages;
    private final GraphQlProperties props;

    public GraphQlExceptionResolver(MessageService messages, GraphQlProperties props) {
        this.messages = messages;
        this.props = props;
    }

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        Mapping m = map(ex);
        return toGraphQlError(m, env);
    }

    private Mapping map(Throwable ex) {
        // Custom hierarchy first
        if (ex instanceof BaseException be) {
            List<ValidationException.FieldViolation> fieldErrors =
                    (be instanceof ValidationException ve) ? ve.getFieldErrors() : List.of();
            return new Mapping(be.getErrorCode(), classify(be.getErrorCode()),
                    resolveMessage(be), be.getDetails(), fieldErrors);
        }
        // Bean-validation
        if (ex instanceof ConstraintViolationException cv) {
            List<ValidationException.FieldViolation> errs = cv.getConstraintViolations().stream()
                    .map(v -> new ValidationException.FieldViolation(
                            v.getPropertyPath().toString(), v.getInvalidValue(), v.getMessage()))
                    .toList();
            return new Mapping(ErrorCode.VALIDATION_FAILED, ErrorType.BAD_REQUEST,
                    msg(ErrorCode.VALIDATION_FAILED), Map.of(), errs);
        }
        // Security
        if (ex instanceof BadCredentialsException) return basic(ErrorCode.INVALID_CREDENTIALS, ErrorType.UNAUTHORIZED);
        if (ex instanceof LockedException)         return basic(ErrorCode.ACCOUNT_LOCKED,      ErrorType.UNAUTHORIZED);
        if (ex instanceof DisabledException)       return basic(ErrorCode.ACCOUNT_DISABLED,    ErrorType.FORBIDDEN);
        if (ex instanceof AccessDeniedException)   return basic(ErrorCode.ACCESS_DENIED,       ErrorType.FORBIDDEN);
        if (ex instanceof AuthenticationException) return basic(ErrorCode.UNAUTHORIZED,        ErrorType.UNAUTHORIZED);
        // JWT
        if (ex instanceof ExpiredJwtException)     return basic(ErrorCode.TOKEN_EXPIRED,       ErrorType.UNAUTHORIZED);
        if (ex instanceof JwtException)            return basic(ErrorCode.TOKEN_INVALID,       ErrorType.UNAUTHORIZED);
        // Persistence
        if (ex instanceof DuplicateKeyException)              return basic(ErrorCode.DUPLICATE_RESOURCE,        ErrorType.BAD_REQUEST);
        if (ex instanceof DataIntegrityViolationException)    return basic(ErrorCode.DATA_INTEGRITY_VIOLATION,  ErrorType.BAD_REQUEST);
        if (ex instanceof OptimisticLockingFailureException)  return basic(ErrorCode.OPTIMISTIC_LOCK,           ErrorType.BAD_REQUEST);
        if (ex instanceof CannotAcquireLockException)         return basic(ErrorCode.RESOURCE_LOCKED,           ErrorType.INTERNAL_ERROR);
        if (ex instanceof EmptyResultDataAccessException)     return basic(ErrorCode.NOT_FOUND,                 ErrorType.NOT_FOUND);
        if (ex instanceof QueryTimeoutException)              return basic(ErrorCode.DATABASE_TIMEOUT,          ErrorType.INTERNAL_ERROR);
        if (ex instanceof DataAccessResourceFailureException) return basic(ErrorCode.DATABASE_UNAVAILABLE,      ErrorType.INTERNAL_ERROR);
        // JVM
        if (ex instanceof IllegalArgumentException)           return basic(ErrorCode.BAD_REQUEST,               ErrorType.BAD_REQUEST);
        if (ex instanceof UnsupportedOperationException)      return basic(ErrorCode.NOT_IMPLEMENTED,           ErrorType.INTERNAL_ERROR);
        // Catch-all (don't leak internals)
        log.error("Unhandled GraphQL data-fetcher exception", ex);
        return basic(ErrorCode.INTERNAL_ERROR, ErrorType.INTERNAL_ERROR);
    }

    private static ErrorType classify(ErrorCode code) {
        return switch (code) {
            case VALIDATION_FAILED, BAD_REQUEST, MALFORMED_JSON, MISSING_PARAMETER,
                 MISSING_HEADER, DUPLICATE_RESOURCE, DATA_INTEGRITY_VIOLATION,
                 OPTIMISTIC_LOCK, BUSINESS_RULE_VIOLATION, PRECONDITION_FAILED -> ErrorType.BAD_REQUEST;
            case UNAUTHORIZED, INVALID_CREDENTIALS, TOKEN_EXPIRED, TOKEN_INVALID,
                 ACCOUNT_LOCKED -> ErrorType.UNAUTHORIZED;
            case FORBIDDEN, ACCESS_DENIED, ACCOUNT_DISABLED,
                 ACCOUNT_EXPIRED, CREDENTIALS_EXPIRED -> ErrorType.FORBIDDEN;
            case NOT_FOUND -> ErrorType.NOT_FOUND;
            default -> ErrorType.INTERNAL_ERROR;
        };
    }

    private Mapping basic(ErrorCode code, ErrorType type) {
        return new Mapping(code, type, msg(code), Map.of(), List.of());
    }

    private String msg(ErrorCode code) {
        return messages.get(code.getMessageKey());
    }

    private String resolveMessage(BaseException be) {
        return messages.getOrDefault(be.getMessageKey(), be.getMessage(), be.getMessageArgs());
    }

    private GraphQLError toGraphQlError(Mapping m, DataFetchingEnvironment env) {
        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("errorCode", m.code.getCode());
        extensions.put("classification", m.code.name());
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) extensions.put("correlationId", correlationId);
        extensions.put("timestamp", Instant.now().toString());
        if (m.details != null && !m.details.isEmpty()) {
            extensions.put("details", new HashMap<>(m.details));
        }
        if (m.fieldErrors != null && !m.fieldErrors.isEmpty()) {
            extensions.put("fieldErrors", m.fieldErrors);
        }

        String clientMessage = props.getErrorMapping().isExposeMessage()
                ? m.message : m.code.name();

        return GraphqlErrorBuilder.newError(env)
                .errorType(m.type)
                .message(clientMessage)
                .extensions(extensions)
                .build();
    }

    private record Mapping(ErrorCode code,
                           ErrorType type,
                           String message,
                           Map<String, Object> details,
                           List<ValidationException.FieldViolation> fieldErrors) {}
}