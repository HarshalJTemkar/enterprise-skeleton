package com.enterprise.common.exception;

/**
 * Thrown when an entity referenced by id / slug / path variable does not
 * exist. Maps to HTTP 404 via {@link ErrorCode#NOT_FOUND}.
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    public ResourceNotFoundException(String resourceType, Object id) {
        super(ErrorCode.NOT_FOUND,
                "%s with id '%s' not found".formatted(resourceType, id));
    }
}
