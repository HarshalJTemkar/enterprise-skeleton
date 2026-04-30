package com.enterprise.common.exception;

import java.io.Serial;
import java.util.Collections;
import java.util.Map;

/**
 * Root of the platform exception hierarchy.
 *
 * <p>Every custom exception thrown by a service should extend this class (or
 * one of its direct children) so the {@code GlobalExceptionHandler} can:</p>
 * <ul>
 *   <li>Map to the right HTTP status via {@link ErrorCode}.</li>
 *   <li>Resolve the localized message via the {@link #getMessageKey()}.</li>
 *   <li>Pass structured {@link #getDetails() details} through to the client.</li>
 * </ul>
 */
public abstract class BaseException extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;
    private final Map<String, Object> details;
    private final Object[] messageArgs;

    /**
     * @param errorCode the catalog entry; its status is emitted in the response
     * @param message   non-localized default message (English)
     */
    protected BaseException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, Collections.emptyMap(), new Object[0]);
    }

    protected BaseException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, cause, Collections.emptyMap(), new Object[0]);
    }

    protected BaseException(ErrorCode errorCode, String message, Throwable cause,
                            Map<String, Object> details, Object... messageArgs) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details == null ? Collections.emptyMap() : Map.copyOf(details);
        this.messageArgs = messageArgs == null ? new Object[0] : messageArgs;
    }

    /** @return the error-catalog entry this exception is bound to. */
    public ErrorCode getErrorCode() { return errorCode; }

    /** @return the i18n message-bundle key for the error (from the code). */
    public String getMessageKey() { return errorCode.getMessageKey(); }

    /** @return arguments substituted into the i18n message (see {@code MessageFormat}). */
    public Object[] getMessageArgs() { return messageArgs.clone(); }

    /** @return arbitrary structured details to expose to the client (never {@code null}). */
    public Map<String, Object> getDetails() { return details; }
}
