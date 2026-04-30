package com.enterprise.common.exception;

/**
 * Thrown when an optimistic-locking failure or pessimistic-lock acquisition
 * failure is detected. Maps to HTTP 409 via {@link ErrorCode#OPTIMISTIC_LOCK}
 * by default.
 */
public class ConcurrencyException extends BaseException {

    public ConcurrencyException(String message) {
        super(ErrorCode.OPTIMISTIC_LOCK, message);
    }

    public ConcurrencyException(String message, Throwable cause) {
        super(ErrorCode.OPTIMISTIC_LOCK, message, cause);
    }

    public ConcurrencyException(ErrorCode code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
