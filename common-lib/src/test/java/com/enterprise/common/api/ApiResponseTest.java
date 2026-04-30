package com.enterprise.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ApiResponseTest {

    @AfterEach
    void clearMdc() { MDC.clear(); }

    @Test
    void ok_carries_data_and_correlation_id_from_mdc() {
        MDC.put("correlationId", "abc-123");
        ApiResponse<String> r = ApiResponse.ok("hello");
        assertThat(r.status()).isEqualTo("success");
        assertThat(r.data()).isEqualTo("hello");
        assertThat(r.message()).isNull();
        assertThat(r.correlationId()).isEqualTo("abc-123");
        assertThat(r.timestamp()).isNotNull();
    }

    @Test
    void ok_with_message_includes_both() {
        ApiResponse<Integer> r = ApiResponse.ok(42, "ok");
        assertThat(r.data()).isEqualTo(42);
        assertThat(r.message()).isEqualTo("ok");
    }

    @Test
    void message_only_has_null_data() {
        ApiResponse<Void> r = ApiResponse.message("done");
        assertThat(r.data()).isNull();
        assertThat(r.message()).isEqualTo("done");
        assertThat(r.status()).isEqualTo("success");
    }
}
