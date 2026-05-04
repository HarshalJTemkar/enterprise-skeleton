package com.enterprise.sample.api;

import com.enterprise.common.api.QueryResponse;
import com.enterprise.common.exception.BusinessException;
import com.enterprise.common.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

/**
 * GraphQL counterpart of {@link SampleController}. Demonstrates how a downstream service exposes
 * the same operations over GraphQL using the shared {@link QueryResponse} envelope from {@code
 * common-lib}.
 *
 * <p>Identity is still established by the API gateway and forwarded as {@code X-Auth-Subject} /
 * {@code X-Auth-Roles}; the resolver receives them as schema arguments (mapped from headers in a
 * higher-level filter, or passed by the client during local testing).
 */
@Controller
@Validated
public class SampleGraphQlController {

  @QueryMapping
  public QueryResponse<WhoAmI> whoAmI(@Argument String subject, @Argument String roles) {
    return QueryResponse.ok(
        new WhoAmI(
            subject == null ? "anonymous" : subject,
            roles == null ? "[]" : roles,
            org.slf4j.MDC.get("correlationId")));
  }

  @MutationMapping
  public QueryResponse<Order> createOrder(
      @Argument @Valid CreateOrderInput input, @Argument String subject) {
    if (input.quantity() <= 0) {
      // Will be mapped by GraphQlExceptionResolver → BAD_REQUEST + ERR-2001
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "quantity must be positive");
    }
    return QueryResponse.ok(
        new Order(
            UUID.randomUUID().toString(),
            subject == null ? "anonymous" : subject,
            input.product(),
            input.quantity()));
  }

  public record WhoAmI(String subject, String roles, String correlationId) {}

  public record Order(String orderId, String createdBy, String product, int quantity) {}

  public record CreateOrderInput(@NotBlank String product, @Min(1) int quantity) {}
}
