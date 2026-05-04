package com.enterprise.sample.api;

import com.enterprise.common.api.ApiResponse;
import com.enterprise.common.idempotency.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demonstrates how a downstream microservice consumes the platform:
 *
 * <ul>
 *   <li>Returns the canonical {@link ApiResponse} envelope.
 *   <li>Trusts {@code X-Auth-Subject} / {@code X-Auth-Roles} headers forwarded by the API gateway
 *       (never validates a JWT itself).
 *   <li>Showcases {@code @Idempotent} on {@code POST /orders}.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/sample")
@Tag(name = "Sample")
public class SampleController {

  @GetMapping("/whoami")
  @Operation(summary = "Echo the gateway-propagated identity headers")
  public ApiResponse<WhoAmI> whoAmI(
      @RequestHeader(value = "X-Auth-Subject", required = false) String subject,
      @RequestHeader(value = "X-Auth-Roles", required = false) String roles,
      HttpServletRequest req) {
    return ApiResponse.ok(
        new WhoAmI(
            subject == null ? "anonymous" : subject,
            roles == null ? "[]" : roles,
            req.getHeader("X-Correlation-ID")));
  }

  @PostMapping("/orders")
  @Idempotent(name = "sample-create-order")
  @Operation(summary = "Idempotent order creation (replay-safe via Idempotency-Key header)")
  public ApiResponse<OrderCreated> createOrder(
      @Valid @RequestBody CreateOrder body,
      @RequestHeader(value = "X-Auth-Subject", required = false) String subject) {
    return ApiResponse.ok(
        new OrderCreated(
            UUID.randomUUID().toString(),
            subject == null ? "anonymous" : subject,
            body.product(),
            body.quantity()));
  }

  public record WhoAmI(String subject, String roles, String correlationId) {}

  public record CreateOrder(@NotBlank String product, int quantity) {}

  public record OrderCreated(String orderId, String createdBy, String product, int quantity) {}
}
