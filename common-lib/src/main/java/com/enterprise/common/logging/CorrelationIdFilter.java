package com.enterprise.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Injects a correlation id into the SLF4J {@link MDC} so every log line emitted for a request can
 * be grouped across threads (including virtual threads). The id is:
 *
 * <ol>
 *   <li>read from the configured header (default {@code X-Correlation-ID});
 *   <li>generated as a UUID if absent;
 *   <li>echoed back on the response for client-side correlation.
 * </ol>
 *
 * <p>When {@link LoggingProperties.CorrelationId#isGenerateRequestId()} is {@code true} a per-hop
 * {@code X-Request-ID} is also generated and stored in MDC under {@code requestId}.
 */
public class CorrelationIdFilter extends OncePerRequestFilter implements Ordered {

  private final LoggingProperties.CorrelationId props;

  public CorrelationIdFilter(LoggingProperties.CorrelationId props) {
    this.props = props;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = props.getHeaderName();
    String id = request.getHeader(header);
    if (id == null || id.isBlank()) id = UUID.randomUUID().toString();

    MDC.put(props.getMdcKey(), id);
    response.setHeader(header, id);

    String requestId = null;
    if (props.isGenerateRequestId()) {
      requestId = UUID.randomUUID().toString();
      MDC.put("requestId", requestId);
      response.setHeader("X-Request-ID", requestId);
    }

    // Also record the request path for every log line.
    MDC.put("requestPath", request.getRequestURI());
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(props.getMdcKey());
      if (requestId != null) MDC.remove("requestId");
      MDC.remove("requestPath");
    }
  }

  /** Run as early as possible so subsequent filters see the MDC values. */
  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
