package com.enterprise.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void generates_id_when_header_missing_and_echoes_it_back() throws Exception {
    var f = new CorrelationIdFilter(new LoggingProperties.CorrelationId());
    var req = new MockHttpServletRequest("GET", "/x");
    var res = new MockHttpServletResponse();
    FilterChain chain =
        (q, s) -> {
          assertThat(MDC.get("correlationId")).isNotBlank();
          assertThat(MDC.get("requestPath")).isEqualTo("/x");
        };
    f.doFilter(req, res, chain);
    assertThat(res.getHeader("X-Correlation-ID")).isNotBlank();
    assertThat(res.getHeader("X-Request-ID")).isNotBlank();
    assertThat(MDC.get("correlationId")).isNull(); // cleared after filter
  }

  @Test
  void reuses_incoming_correlation_id_header() throws Exception {
    var f = new CorrelationIdFilter(new LoggingProperties.CorrelationId());
    var req = new MockHttpServletRequest("GET", "/y");
    req.addHeader("X-Correlation-ID", "abc");
    var res = new MockHttpServletResponse();
    f.doFilter(req, res, (q, s) -> assertThat(MDC.get("correlationId")).isEqualTo("abc"));
    assertThat(res.getHeader("X-Correlation-ID")).isEqualTo("abc");
  }
}
