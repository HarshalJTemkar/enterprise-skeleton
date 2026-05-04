package com.enterprise.common.exception;

/**
 * Thrown when calling an external system (another micro-service, a third-party API, a DB) fails.
 * Maps to HTTP 502 / 503 / 504 depending on the {@link ErrorCode} used.
 */
public class IntegrationException extends BaseException {

  public IntegrationException(String message) {
    super(ErrorCode.INTEGRATION_ERROR, message);
  }

  public IntegrationException(String message, Throwable cause) {
    super(ErrorCode.INTEGRATION_ERROR, message, cause);
  }

  public IntegrationException(ErrorCode errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }

  /** Convenience factory for timeouts. */
  public static IntegrationException timeout(String target) {
    return new IntegrationException(
        ErrorCode.INTEGRATION_TIMEOUT, "Timed out calling " + target, null);
  }

  /** Convenience factory when a downstream is unreachable. */
  public static IntegrationException unavailable(String target, Throwable cause) {
    return new IntegrationException(
        ErrorCode.INTEGRATION_UNAVAILABLE, target + " is unavailable", cause);
  }
}
