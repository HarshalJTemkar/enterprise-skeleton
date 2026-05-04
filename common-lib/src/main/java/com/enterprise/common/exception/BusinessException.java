package com.enterprise.common.exception;

import java.util.Map;

/**
 * Thrown when a business rule is violated. Defaults to HTTP 422 via {@link
 * ErrorCode#BUSINESS_RULE_VIOLATION}; callers can pass a more specific {@link ErrorCode} where
 * appropriate.
 */
public class BusinessException extends BaseException {

  /** Creates an exception with the default business-rule error code. */
  public BusinessException(String message) {
    super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
  }

  /** Creates an exception bound to a specific error-catalog entry. */
  public BusinessException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }

  /** Include structured details that will surface under {@code problem.details}. */
  public BusinessException(
      ErrorCode errorCode, String message, Map<String, Object> details, Object... args) {
    super(errorCode, message, null, details, args);
  }

  /**
   * @deprecated Misleading: the {@code code} string is ignored, and the exception always maps to
   *     {@link ErrorCode#BUSINESS_RULE_VIOLATION} (422). Use {@link #BusinessException(ErrorCode,
   *     String)} instead so the response status reflects the intended error.
   */
  @Deprecated(forRemoval = true)
  public BusinessException(String code, String message) {
    super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
  }

  /**
   * @deprecated Use {@link #getErrorCode()} for the structured code.
   */
  @Deprecated
  public String getCode() {
    return getErrorCode().getCode();
  }
}
