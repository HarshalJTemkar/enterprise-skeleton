package com.enterprise.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Central catalog of business error codes. Each code carries:
 *
 * <ul>
 *   <li>a stable machine-readable code (exposed to clients)
 *   <li>a default message-bundle key (resolved via {@code MessageService})
 *   <li>an HTTP status to emit when the exception bubbles up
 * </ul>
 *
 * <p>Keep codes immutable once released; add new ones rather than renumbering.
 */
public enum ErrorCode {

  // ---- Generic ----
  INTERNAL_ERROR("ERR-0001", "error.internal", HttpStatus.INTERNAL_SERVER_ERROR),
  VALIDATION_FAILED("ERR-0002", "error.validation", HttpStatus.BAD_REQUEST),
  BAD_REQUEST("ERR-0003", "error.bad.request", HttpStatus.BAD_REQUEST),
  NOT_FOUND("ERR-0004", "error.not.found", HttpStatus.NOT_FOUND),
  CONFLICT("ERR-0005", "error.conflict", HttpStatus.CONFLICT),
  METHOD_NOT_ALLOWED("ERR-0006", "error.method.not.allowed", HttpStatus.METHOD_NOT_ALLOWED),
  UNSUPPORTED_MEDIA_TYPE(
      "ERR-0007", "error.unsupported.media.type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
  NOT_ACCEPTABLE("ERR-0008", "error.not.acceptable", HttpStatus.NOT_ACCEPTABLE),
  PAYLOAD_TOO_LARGE("ERR-0009", "error.payload.too.large", HttpStatus.PAYLOAD_TOO_LARGE),
  MISSING_PARAMETER("ERR-0010", "error.missing.parameter", HttpStatus.BAD_REQUEST),
  MISSING_HEADER("ERR-0011", "error.missing.header", HttpStatus.BAD_REQUEST),
  MALFORMED_JSON("ERR-0012", "error.malformed.json", HttpStatus.BAD_REQUEST),
  NOT_IMPLEMENTED("ERR-0013", "error.not.implemented", HttpStatus.NOT_IMPLEMENTED),
  REQUEST_TIMEOUT("ERR-0014", "error.request.timeout", HttpStatus.REQUEST_TIMEOUT),

  // ---- Security ----
  UNAUTHORIZED("ERR-1001", "error.unauthorized", HttpStatus.UNAUTHORIZED),
  FORBIDDEN("ERR-1002", "error.forbidden", HttpStatus.FORBIDDEN),
  INVALID_CREDENTIALS("ERR-1003", "error.invalid.credentials", HttpStatus.UNAUTHORIZED),
  TOKEN_EXPIRED("ERR-1004", "error.token.expired", HttpStatus.UNAUTHORIZED),
  TOKEN_INVALID("ERR-1005", "error.token.invalid", HttpStatus.UNAUTHORIZED),
  ACCESS_DENIED("ERR-1006", "error.access.denied", HttpStatus.FORBIDDEN),
  ACCOUNT_LOCKED("ERR-1007", "error.account.locked", HttpStatus.LOCKED),
  ACCOUNT_DISABLED("ERR-1008", "error.account.disabled", HttpStatus.FORBIDDEN),
  ACCOUNT_EXPIRED("ERR-1009", "error.account.expired", HttpStatus.FORBIDDEN),
  CREDENTIALS_EXPIRED("ERR-1010", "error.credentials.expired", HttpStatus.FORBIDDEN),

  // ---- Business ----
  BUSINESS_RULE_VIOLATION("ERR-2001", "error.business.rule", HttpStatus.UNPROCESSABLE_ENTITY),
  RESOURCE_LOCKED("ERR-2002", "error.resource.locked", HttpStatus.LOCKED),
  RESOURCE_EXPIRED("ERR-2003", "error.resource.expired", HttpStatus.GONE),
  DUPLICATE_RESOURCE("ERR-2004", "error.duplicate.resource", HttpStatus.CONFLICT),
  OPTIMISTIC_LOCK("ERR-2005", "error.optimistic.lock", HttpStatus.CONFLICT),
  PRECONDITION_FAILED("ERR-2006", "error.precondition.failed", HttpStatus.PRECONDITION_FAILED),

  // ---- Persistence / database ----
  DATABASE_ERROR("ERR-5001", "error.database", HttpStatus.INTERNAL_SERVER_ERROR),
  DATABASE_UNAVAILABLE("ERR-5002", "error.database.unavailable", HttpStatus.SERVICE_UNAVAILABLE),
  DATABASE_TIMEOUT("ERR-5003", "error.database.timeout", HttpStatus.GATEWAY_TIMEOUT),
  DATA_INTEGRITY_VIOLATION("ERR-5004", "error.data.integrity", HttpStatus.CONFLICT),

  // ---- Integration ----
  INTEGRATION_ERROR("ERR-3001", "error.integration", HttpStatus.BAD_GATEWAY),
  INTEGRATION_TIMEOUT("ERR-3002", "error.integration.timeout", HttpStatus.GATEWAY_TIMEOUT),
  INTEGRATION_UNAVAILABLE(
      "ERR-3003", "error.integration.unavailable", HttpStatus.SERVICE_UNAVAILABLE),
  CIRCUIT_OPEN("ERR-3004", "error.circuit.open", HttpStatus.SERVICE_UNAVAILABLE),
  BULKHEAD_FULL("ERR-3005", "error.bulkhead.full", HttpStatus.SERVICE_UNAVAILABLE),

  // ---- Rate limiting / idempotency ----
  TOO_MANY_REQUESTS("ERR-4001", "error.too.many.requests", HttpStatus.TOO_MANY_REQUESTS),
  IDEMPOTENCY_REPLAY("ERR-4002", "error.idempotency.replay", HttpStatus.CONFLICT);

  private final String code;
  private final String messageKey;
  private final HttpStatus httpStatus;

  ErrorCode(String code, String messageKey, HttpStatus httpStatus) {
    this.code = code;
    this.messageKey = messageKey;
    this.httpStatus = httpStatus;
  }

  public String getCode() {
    return code;
  }

  public String getMessageKey() {
    return messageKey;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
