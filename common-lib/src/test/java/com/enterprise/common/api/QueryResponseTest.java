package com.enterprise.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class QueryResponseTest {

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void ok_carries_data_and_correlation_id() {
    MDC.put("correlationId", "cid-1");
    QueryResponse<String> r = QueryResponse.ok("hello");
    assertThat(r.status()).isEqualTo("success");
    assertThat(r.data()).isEqualTo("hello");
    assertThat(r.correlationId()).isEqualTo("cid-1");
    assertThat(r.warnings()).isEmpty();
    assertThat(r.extensions()).isEmpty();
    assertThat(r.timestamp()).isNotNull();
  }

  @Test
  void ok_with_message_includes_both() {
    QueryResponse<Integer> r = QueryResponse.ok(42, "answered");
    assertThat(r.data()).isEqualTo(42);
    assertThat(r.message()).isEqualTo("answered");
  }

  @Test
  void ok_with_warnings_passes_warnings_through() {
    QueryResponse<String> r =
        QueryResponse.okWithWarnings("partial", List.of("one shard timed out"));
    assertThat(r.warnings()).containsExactly("one shard timed out");
  }

  @Test
  void ok_with_extensions_passes_metadata() {
    QueryResponse<String> r = QueryResponse.okWithExtensions("ok", Map.of("traceId", "t1"));
    assertThat(r.extensions()).containsEntry("traceId", "t1");
  }

  @Test
  void empty_returns_null_data_with_success_status() {
    QueryResponse<String> r = QueryResponse.empty();
    assertThat(r.data()).isNull();
    assertThat(r.status()).isEqualTo("success");
  }

  @Test
  void message_only_response_has_null_data() {
    QueryResponse<Void> r = QueryResponse.message("done");
    assertThat(r.data()).isNull();
    assertThat(r.message()).isEqualTo("done");
  }
}
