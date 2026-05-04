package com.enterprise.common.exception;

/**
 * Thrown for authentication / authorization failures originating from application code. Named
 * {@code SecurityAppException} to avoid clashing with {@code java.lang.SecurityException}.
 */
public class SecurityAppException extends BaseException {

  public SecurityAppException(String message) {
    super(ErrorCode.UNAUTHORIZED, message);
  }

  public SecurityAppException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  public static SecurityAppException unauthorized(String msg) {
    return new SecurityAppException(ErrorCode.UNAUTHORIZED, msg);
  }

  public static SecurityAppException forbidden(String msg) {
    return new SecurityAppException(ErrorCode.FORBIDDEN, msg);
  }

  public static SecurityAppException invalidCredentials() {
    return new SecurityAppException(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password");
  }
}
