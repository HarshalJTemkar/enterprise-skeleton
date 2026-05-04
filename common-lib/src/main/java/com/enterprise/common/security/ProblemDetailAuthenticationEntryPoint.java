package com.enterprise.common.security;

import com.enterprise.common.api.ErrorResponse;
import com.enterprise.common.enums.HttpHeaderConstants;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.common.i18n.MessageService;
import com.enterprise.common.util.JsonUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Bridges Spring Security's authentication / authorization failures into the same RFC 7807 {@link
 * ProblemDetail} body used by the rest of the platform.
 *
 * <p>Spring Security intercepts {@link AuthenticationException} (401) and {@link
 * AccessDeniedException} (403) <em>before</em> the {@code @RestControllerAdvice} ever runs, so we
 * have to render the response here too.
 */
public class ProblemDetailAuthenticationEntryPoint
    implements AuthenticationEntryPoint, AccessDeniedHandler {

  private static final String PROBLEM_BASE = "https://errors.enterprise.com/";
  private final MessageService messageService;

  public ProblemDetailAuthenticationEntryPoint(MessageService messageService) {
    this.messageService = messageService;
  }

  /** 401 path – unauthenticated. */
  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException, ServletException {
    write(response, request, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, ex.getMessage());
  }

  /** 403 path – authenticated but not allowed. */
  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException, ServletException {
    write(response, request, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, ex.getMessage());
  }

  /** Build the {@link ProblemDetail} payload and stream it to the client. */
  private void write(
      HttpServletResponse response,
      HttpServletRequest request,
      HttpStatus status,
      ErrorCode code,
      String detail)
      throws IOException {
    String correlationId = MDC.get("correlationId");
    String localized = messageService.getOrDefault(code.getMessageKey(), detail);

    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, localized);
    pd.setTitle(code.name());
    pd.setType(URI.create(PROBLEM_BASE + code.getCode()));
    pd.setInstance(URI.create(request.getRequestURI()));
    pd.setProperty(
        "error",
        new ErrorResponse(
            code.getCode(),
            localized,
            correlationId,
            Instant.now(),
            request.getRequestURI(),
            List.of(),
            Map.of()));

    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    if (correlationId != null) {
      response.setHeader(HttpHeaderConstants.CORRELATION_ID, correlationId);
    }
    response.getWriter().write(JsonUtils.toJson(pd));
  }
}
