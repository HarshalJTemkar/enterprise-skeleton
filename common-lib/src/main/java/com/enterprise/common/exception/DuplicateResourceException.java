package com.enterprise.common.exception;

import java.util.Map;

/**
 * Thrown when an attempt is made to create a resource whose unique
 * identifier (or business key) already exists. Maps to HTTP 409 via
 * {@link ErrorCode#DUPLICATE_RESOURCE}.
 */
public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }

    public DuplicateResourceException(String resourceType, String field, Object value) {
        super(ErrorCode.DUPLICATE_RESOURCE,
                "%s with %s='%s' already exists".formatted(resourceType, field, value),
                null,
                Map.of("resource", resourceType, "field", field, "value", String.valueOf(value)));
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(ErrorCode.DUPLICATE_RESOURCE, message, cause);
    }
}
