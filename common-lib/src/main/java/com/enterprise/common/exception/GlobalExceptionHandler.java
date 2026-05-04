package com.enterprise.common.exception;

import com.enterprise.common.api.ErrorResponse;
import com.enterprise.common.enums.HttpHeaderConstants;
import com.enterprise.common.i18n.MessageService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Global REST exception handler that returns RFC 7807 {@link ProblemDetail} documents with an
 * embedded {@link ErrorResponse} envelope.
 *
 * <p>Handlers are ordered from most-specific to most-generic. Spring picks the closest match by
 * parameter type, so the catch-all {@link Exception} handler only fires for genuinely unexpected
 * errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String PROBLEM_BASE = "https://errors.enterprise.com/";

  private final MessageService messageService;

  public GlobalExceptionHandler(MessageService messageService) {
    this.messageService = messageService;
  }

  // ===================================================================
  // Custom hierarchy
  // ===================================================================

  /** Handles any {@link BaseException} (root of the custom hierarchy). */
  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ProblemDetail> handleBase(BaseException ex, HttpServletRequest req) {
    HttpStatus status = ex.getErrorCode().getHttpStatus();
    String localized =
        messageService.getOrDefault(ex.getMessageKey(), ex.getMessage(), ex.getMessageArgs());
    List<ValidationException.FieldViolation> fieldErrors =
        (ex instanceof ValidationException ve) ? ve.getFieldErrors() : List.of();
    return build(status, ex.getErrorCode(), localized, req, fieldErrors, ex.getDetails(), ex);
  }

  // ===================================================================
  // Bean validation
  // ===================================================================

  /** {@code @Valid} on a request body. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleMethodArgNotValid(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    List<ValidationException.FieldViolation> violations =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fe ->
                    new ValidationException.FieldViolation(
                        fe.getField(), fe.getRejectedValue(), fe.getDefaultMessage()))
            .toList();
    return build(
        HttpStatus.BAD_REQUEST,
        ErrorCode.VALIDATION_FAILED,
        messageService.get(ErrorCode.VALIDATION_FAILED.getMessageKey()),
        req,
        violations,
        Map.of(),
        ex);
  }

  /** {@code @Validated} on a method parameter. */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraint(
      ConstraintViolationException ex, HttpServletRequest req) {
    List<ValidationException.FieldViolation> violations =
        ex.getConstraintViolations().stream()
            .map(
                cv ->
                    new ValidationException.FieldViolation(
                        cv.getPropertyPath().toString(), cv.getInvalidValue(), cv.getMessage()))
            .toList();
    return build(
        HttpStatus.BAD_REQUEST,
        ErrorCode.VALIDATION_FAILED,
        messageService.get(ErrorCode.VALIDATION_FAILED.getMessageKey()),
        req,
        violations,
        Map.of(),
        ex);
  }

  // ===================================================================
  // Request parsing / binding
  // ===================================================================

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleUnreadable(
      HttpMessageNotReadableException ex, HttpServletRequest req) {
    return build(
        HttpStatus.BAD_REQUEST,
        ErrorCode.MALFORMED_JSON,
        "Malformed or unreadable request body",
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
    return build(
        HttpStatus.BAD_REQUEST,
        ErrorCode.BAD_REQUEST,
        "Parameter '" + ex.getName() + "' has invalid value",
        req,
        List.of(),
        Map.of(
            "parameter",
            ex.getName(),
            "expectedType",
            ex.getRequiredType() == null ? "?" : ex.getRequiredType().getSimpleName()),
        ex);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ProblemDetail> handleMissingParam(
      MissingServletRequestParameterException ex, HttpServletRequest req) {
    return build(
        HttpStatus.BAD_REQUEST,
        ErrorCode.MISSING_PARAMETER,
        "Required parameter '" + ex.getParameterName() + "' is missing",
        req,
        List.of(),
        Map.of("parameter", ex.getParameterName()),
        ex);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ProblemDetail> handleMissingHeader(
      MissingRequestHeaderException ex, HttpServletRequest req) {
    return build(
        HttpStatus.BAD_REQUEST,
        ErrorCode.MISSING_HEADER,
        "Required header '" + ex.getHeaderName() + "' is missing",
        req,
        List.of(),
        Map.of("header", ex.getHeaderName()),
        ex);
  }

  @ExceptionHandler(MissingPathVariableException.class)
  public ResponseEntity<ProblemDetail> handleMissingPathVar(
      MissingPathVariableException ex, HttpServletRequest req) {
    return build(
        HttpStatus.BAD_REQUEST,
        ErrorCode.BAD_REQUEST,
        "Missing path variable '" + ex.getVariableName() + "'",
        req,
        List.of(),
        Map.of("variable", ex.getVariableName()),
        ex);
  }

  @ExceptionHandler(ServletRequestBindingException.class)
  public ResponseEntity<ProblemDetail> handleBindingException(
      ServletRequestBindingException ex, HttpServletRequest req) {
    return build(
        HttpStatus.BAD_REQUEST,
        ErrorCode.BAD_REQUEST,
        ex.getMessage(),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  // ===================================================================
  // HTTP-level
  // ===================================================================

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ProblemDetail> handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
    return build(
        HttpStatus.METHOD_NOT_ALLOWED,
        ErrorCode.METHOD_NOT_ALLOWED,
        messageService.get(ErrorCode.METHOD_NOT_ALLOWED.getMessageKey()),
        req,
        List.of(),
        Map.of(
            "supported",
            String.join(
                ",", ex.getSupportedMethods() == null ? new String[0] : ex.getSupportedMethods())),
        ex);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ProblemDetail> handleUnsupportedMediaType(
      HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
    return build(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        ErrorCode.UNSUPPORTED_MEDIA_TYPE,
        messageService.get(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessageKey()),
        req,
        List.of(),
        Map.of("contentType", String.valueOf(ex.getContentType())),
        ex);
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<ProblemDetail> handleNotAcceptable(
      HttpMediaTypeNotAcceptableException ex, HttpServletRequest req) {
    return build(
        HttpStatus.NOT_ACCEPTABLE,
        ErrorCode.NOT_ACCEPTABLE,
        messageService.get(ErrorCode.NOT_ACCEPTABLE.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ProblemDetail> handleNoHandler(
      NoHandlerFoundException ex, HttpServletRequest req) {
    return build(
        HttpStatus.NOT_FOUND,
        ErrorCode.NOT_FOUND,
        messageService.get(ErrorCode.NOT_FOUND.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ProblemDetail> handleMaxUpload(
      MaxUploadSizeExceededException ex, HttpServletRequest req) {
    return build(
        HttpStatus.PAYLOAD_TOO_LARGE,
        ErrorCode.PAYLOAD_TOO_LARGE,
        messageService.get(ErrorCode.PAYLOAD_TOO_LARGE.getMessageKey()),
        req,
        List.of(),
        Map.of("maxBytes", ex.getMaxUploadSize()),
        ex);
  }

  @ExceptionHandler(MultipartException.class)
  public ResponseEntity<ProblemDetail> handleMultipart(
      MultipartException ex, HttpServletRequest req) {
    return build(
        HttpStatus.BAD_REQUEST,
        ErrorCode.BAD_REQUEST,
        "Malformed multipart request",
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler(AsyncRequestTimeoutException.class)
  public ResponseEntity<ProblemDetail> handleAsyncTimeout(
      AsyncRequestTimeoutException ex, HttpServletRequest req) {
    return build(
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorCode.INTEGRATION_TIMEOUT,
        "Async request timed out",
        req,
        List.of(),
        Map.of(),
        ex);
  }

  /** Honours {@link ResponseStatusException#getStatusCode()} verbatim. */
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ProblemDetail> handleResponseStatus(
      ResponseStatusException ex, HttpServletRequest req) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    ErrorCode code = mapStatusToCode(status);
    String reason =
        ex.getReason() != null ? ex.getReason() : messageService.get(code.getMessageKey());
    return build(status, code, reason, req, List.of(), Map.of(), ex);
  }

  // ===================================================================
  // Spring Security
  // ===================================================================

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ProblemDetail> handleBadCredentials(
      BadCredentialsException ex, HttpServletRequest req) {
    return build(
        HttpStatus.UNAUTHORIZED,
        ErrorCode.INVALID_CREDENTIALS,
        messageService.get(ErrorCode.INVALID_CREDENTIALS.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler(LockedException.class)
  public ResponseEntity<ProblemDetail> handleLocked(LockedException ex, HttpServletRequest req) {
    return build(
        HttpStatus.LOCKED,
        ErrorCode.ACCOUNT_LOCKED,
        messageService.get(ErrorCode.ACCOUNT_LOCKED.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler(DisabledException.class)
  public ResponseEntity<ProblemDetail> handleDisabled(
      DisabledException ex, HttpServletRequest req) {
    return build(
        HttpStatus.FORBIDDEN,
        ErrorCode.ACCOUNT_DISABLED,
        messageService.get(ErrorCode.ACCOUNT_DISABLED.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler(AccountExpiredException.class)
  public ResponseEntity<ProblemDetail> handleAccountExpired(
      AccountExpiredException ex, HttpServletRequest req) {
    return build(
        HttpStatus.FORBIDDEN,
        ErrorCode.ACCOUNT_EXPIRED,
        messageService.get(ErrorCode.ACCOUNT_EXPIRED.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler(CredentialsExpiredException.class)
  public ResponseEntity<ProblemDetail> handleCredentialsExpired(
      CredentialsExpiredException ex, HttpServletRequest req) {
    return build(
        HttpStatus.FORBIDDEN,
        ErrorCode.CREDENTIALS_EXPIRED,
        messageService.get(ErrorCode.CREDENTIALS_EXPIRED.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest req) {
    return build(
        HttpStatus.FORBIDDEN,
        ErrorCode.ACCESS_DENIED,
        messageService.get(ErrorCode.ACCESS_DENIED.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  /** Catch-all for any other Spring Security {@code AuthenticationException}. */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ProblemDetail> handleAuth(
      AuthenticationException ex, HttpServletRequest req) {
    return build(
        HttpStatus.UNAUTHORIZED,
        ErrorCode.UNAUTHORIZED,
        messageService.get(ErrorCode.UNAUTHORIZED.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  // ===================================================================
  // JWT (jjwt)
  // ===================================================================

  @ExceptionHandler(ExpiredJwtException.class)
  public ResponseEntity<ProblemDetail> handleExpiredJwt(
      ExpiredJwtException ex, HttpServletRequest req) {
    return build(
        HttpStatus.UNAUTHORIZED,
        ErrorCode.TOKEN_EXPIRED,
        messageService.get(ErrorCode.TOKEN_EXPIRED.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler({
    MalformedJwtException.class,
    UnsupportedJwtException.class,
    SignatureException.class,
    JwtException.class
  })
  public ResponseEntity<ProblemDetail> handleInvalidJwt(JwtException ex, HttpServletRequest req) {
    return build(
        HttpStatus.UNAUTHORIZED,
        ErrorCode.TOKEN_INVALID,
        messageService.get(ErrorCode.TOKEN_INVALID.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  // ===================================================================
  // Persistence / database
  // ===================================================================

  /**
   * Duplicate keys / FK / NOT NULL constraint failures. We attempt to surface the constraint name
   * to help operators map the issue.
   */
  @ExceptionHandler({
    DataIntegrityViolationException.class,
    DuplicateKeyException.class,
    SQLIntegrityConstraintViolationException.class
  })
  public ResponseEntity<ProblemDetail> handleIntegrity(Exception ex, HttpServletRequest req) {
    ErrorCode code =
        (ex instanceof DuplicateKeyException)
            ? ErrorCode.DUPLICATE_RESOURCE
            : ErrorCode.DATA_INTEGRITY_VIOLATION;
    String constraint = extractConstraintName(ex);
    Map<String, Object> details = constraint == null ? Map.of() : Map.of("constraint", constraint);
    return build(
        code.getHttpStatus(),
        code,
        messageService.get(code.getMessageKey()),
        req,
        List.of(),
        details,
        ex);
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> handleOptimisticLock(
      OptimisticLockingFailureException ex, HttpServletRequest req) {
    return build(
        HttpStatus.CONFLICT,
        ErrorCode.OPTIMISTIC_LOCK,
        messageService.get(ErrorCode.OPTIMISTIC_LOCK.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler({CannotAcquireLockException.class, PessimisticLockingFailureException.class})
  public ResponseEntity<ProblemDetail> handleLock(Exception ex, HttpServletRequest req) {
    return build(
        HttpStatus.LOCKED,
        ErrorCode.RESOURCE_LOCKED,
        messageService.get(ErrorCode.RESOURCE_LOCKED.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler(EmptyResultDataAccessException.class)
  public ResponseEntity<ProblemDetail> handleEmptyResult(
      EmptyResultDataAccessException ex, HttpServletRequest req) {
    return build(
        HttpStatus.NOT_FOUND,
        ErrorCode.NOT_FOUND,
        messageService.get(ErrorCode.NOT_FOUND.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler(QueryTimeoutException.class)
  public ResponseEntity<ProblemDetail> handleQueryTimeout(
      QueryTimeoutException ex, HttpServletRequest req) {
    return build(
        HttpStatus.GATEWAY_TIMEOUT,
        ErrorCode.DATABASE_TIMEOUT,
        messageService.get(ErrorCode.DATABASE_TIMEOUT.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler(DataAccessResourceFailureException.class)
  public ResponseEntity<ProblemDetail> handleDbDown(
      DataAccessResourceFailureException ex, HttpServletRequest req) {
    log.error("Database resource failure", ex);
    return build(
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorCode.DATABASE_UNAVAILABLE,
        messageService.get(ErrorCode.DATABASE_UNAVAILABLE.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  /** Generic Spring DataAccess catch-all (lower priority than the above). */
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ProblemDetail> handleDataAccess(
      DataAccessException ex, HttpServletRequest req) {
    log.error("DataAccessException", ex);
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.DATABASE_ERROR,
        messageService.get(ErrorCode.DATABASE_ERROR.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler(SQLException.class)
  public ResponseEntity<ProblemDetail> handleSql(SQLException ex, HttpServletRequest req) {
    log.error("SQLException [state={}, code={}]", ex.getSQLState(), ex.getErrorCode(), ex);
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.DATABASE_ERROR,
        messageService.get(ErrorCode.DATABASE_ERROR.getMessageKey()),
        req,
        List.of(),
        Map.of("sqlState", String.valueOf(ex.getSQLState())),
        ex);
  }

  // ===================================================================
  // Resilience4j handlers live in ResilienceExceptionAdvice (conditional)
  // ===================================================================

  // ===================================================================
  // JVM / language-level
  // ===================================================================

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetail> handleIllegalArg(
      IllegalArgumentException ex, HttpServletRequest req) {
    return build(
        HttpStatus.BAD_REQUEST,
        ErrorCode.BAD_REQUEST,
        ex.getMessage() == null ? "Illegal argument" : ex.getMessage(),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ProblemDetail> handleIllegalState(
      IllegalStateException ex, HttpServletRequest req) {
    log.warn("IllegalStateException at {}: {}", req.getRequestURI(), ex.getMessage());
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.INTERNAL_ERROR,
        ex.getMessage() == null ? "Illegal state" : ex.getMessage(),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler(UnsupportedOperationException.class)
  public ResponseEntity<ProblemDetail> handleUnsupported(
      UnsupportedOperationException ex, HttpServletRequest req) {
    return build(
        HttpStatus.NOT_IMPLEMENTED,
        ErrorCode.NOT_IMPLEMENTED,
        messageService.get(ErrorCode.NOT_IMPLEMENTED.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  @ExceptionHandler({TimeoutException.class})
  public ResponseEntity<ProblemDetail> handleTimeout(Exception ex, HttpServletRequest req) {
    return build(
        HttpStatus.GATEWAY_TIMEOUT,
        ErrorCode.INTEGRATION_TIMEOUT,
        messageService.get(ErrorCode.INTEGRATION_TIMEOUT.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  // ===================================================================
  // Catch-all
  // ===================================================================

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnknown(Exception ex, HttpServletRequest req) {
    log.error("Unhandled exception", ex);
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.INTERNAL_ERROR,
        messageService.get(ErrorCode.INTERNAL_ERROR.getMessageKey()),
        req,
        List.of(),
        Map.of(),
        ex);
  }

  // ===================================================================
  // Helpers
  // ===================================================================

  private static ErrorCode mapStatusToCode(HttpStatus status) {
    return switch (status) {
      case BAD_REQUEST -> ErrorCode.BAD_REQUEST;
      case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
      case FORBIDDEN -> ErrorCode.FORBIDDEN;
      case NOT_FOUND -> ErrorCode.NOT_FOUND;
      case METHOD_NOT_ALLOWED -> ErrorCode.METHOD_NOT_ALLOWED;
      case NOT_ACCEPTABLE -> ErrorCode.NOT_ACCEPTABLE;
      case CONFLICT -> ErrorCode.CONFLICT;
      case UNSUPPORTED_MEDIA_TYPE -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
      case PAYLOAD_TOO_LARGE -> ErrorCode.PAYLOAD_TOO_LARGE;
      case UNPROCESSABLE_ENTITY -> ErrorCode.BUSINESS_RULE_VIOLATION;
      case LOCKED -> ErrorCode.RESOURCE_LOCKED;
      case TOO_MANY_REQUESTS -> ErrorCode.TOO_MANY_REQUESTS;
      case NOT_IMPLEMENTED -> ErrorCode.NOT_IMPLEMENTED;
      case BAD_GATEWAY -> ErrorCode.INTEGRATION_ERROR;
      case SERVICE_UNAVAILABLE -> ErrorCode.INTEGRATION_UNAVAILABLE;
      case GATEWAY_TIMEOUT -> ErrorCode.INTEGRATION_TIMEOUT;
      default -> ErrorCode.INTERNAL_ERROR;
    };
  }

  /** Best-effort extraction of a DB constraint name from a chained cause. */
  private static String extractConstraintName(Throwable t) {
    Throwable cur = t;
    while (cur != null) {
      String msg = cur.getMessage();
      if (msg != null) {
        int i = msg.toLowerCase().indexOf("constraint");
        if (i >= 0) {
          String tail = msg.substring(i);
          int q1 = tail.indexOf('"');
          int q2 = q1 >= 0 ? tail.indexOf('"', q1 + 1) : -1;
          if (q1 >= 0 && q2 > q1) return tail.substring(q1 + 1, q2);
        }
      }
      cur = cur.getCause();
    }
    return null;
  }

  /**
   * Builds the final {@link ProblemDetail} response with the correlation-id header and the {@link
   * ErrorResponse} envelope attached under {@code "error"}.
   */
  static ResponseEntity<ProblemDetail> buildResponse(
      HttpStatus status,
      ErrorCode code,
      String message,
      HttpServletRequest req,
      List<ValidationException.FieldViolation> fieldErrors,
      Map<String, Object> details) {
    String path = req == null ? null : req.getRequestURI();
    log.warn("[{}] {} at {} - {}", code.getCode(), status.value(), path, message);

    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, message);
    pd.setTitle(code.name());
    pd.setType(URI.create(PROBLEM_BASE + code.getCode()));
    if (path != null) pd.setInstance(URI.create(path));

    String correlationId = MDC.get("correlationId");
    ErrorResponse envelope =
        new ErrorResponse(
            code.getCode(),
            message,
            correlationId,
            Instant.now(),
            path,
            fieldErrors == null ? List.of() : fieldErrors,
            details);
    pd.setProperty("error", envelope);

    ResponseEntity.BodyBuilder builder =
        ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON);
    if (correlationId != null) builder.header(HttpHeaderConstants.CORRELATION_ID, correlationId);
    return builder.body(pd);
  }

  private ResponseEntity<ProblemDetail> build(
      HttpStatus status,
      ErrorCode code,
      String message,
      HttpServletRequest req,
      List<ValidationException.FieldViolation> fieldErrors,
      Map<String, Object> details,
      Exception ex) {
    return buildResponse(status, code, message, req, fieldErrors, details);
  }
}
