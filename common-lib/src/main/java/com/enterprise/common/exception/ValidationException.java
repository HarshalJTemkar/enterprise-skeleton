package com.enterprise.common.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Thrown when input validation fails. Carries one or more
 * {@link FieldViolation}s which are rendered under {@code errors} in the
 * RFC 7807 response body.
 */
public class ValidationException extends BaseException {

    private final List<FieldViolation> fieldErrors;

    public ValidationException(String message) {
        this(message, List.of());
    }

    public ValidationException(String message, List<FieldViolation> fieldErrors) {
        super(ErrorCode.VALIDATION_FAILED, message);
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    /** @return an unmodifiable list of field-level violations. */
    public List<FieldViolation> getFieldErrors() {
        return fieldErrors;
    }

    /** Fluent builder to collect violations before throwing. */
    public static Builder builder(String message) {
        return new Builder(message);
    }

    /** One field-level violation collected during request validation. */
    public record FieldViolation(String field, Object rejectedValue, String message) {}

    public static final class Builder {
        private final String message;
        private final List<FieldViolation> violations = new ArrayList<>();
        private Builder(String message) { this.message = message; }

        public Builder add(String field, Object rejectedValue, String msg) {
            violations.add(new FieldViolation(field, rejectedValue, msg));
            return this;
        }

        public Builder add(String field, String msg) {
            return add(field, null, msg);
        }

        public ValidationException build() {
            return new ValidationException(message, violations);
        }
    }
}
