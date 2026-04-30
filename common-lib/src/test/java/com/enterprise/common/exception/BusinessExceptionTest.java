package com.enterprise.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BusinessExceptionTest {

    @Test
    void exposes_message_and_default_business_code() {
        var e = new BusinessException("FOO", "bar");
        assertThat(e.getMessage()).isEqualTo("bar");
        // Legacy String-code ctor maps to BUSINESS_RULE_VIOLATION
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
        assertThat(e.getCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION.getCode());
    }

    @Test
    void typed_constructor_carries_supplied_error_code() {
        var e = new BusinessException(ErrorCode.NOT_FOUND, "missing");
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void resource_not_found_includes_resource_and_id() {
        var e = new ResourceNotFoundException("User", "42");
        assertThat(e.getMessage()).contains("User", "42");
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }
}