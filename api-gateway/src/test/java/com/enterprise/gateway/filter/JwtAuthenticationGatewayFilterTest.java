package com.enterprise.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.enterprise.common.config.CommonProperties;
import com.enterprise.common.security.JwtTokenProvider;
import com.enterprise.gateway.config.GatewayJwtProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class JwtAuthenticationGatewayFilterTest {

  private static final String SECRET = "test-secret-test-secret-test-secret-test-secret";

  private CommonProperties props;
  private JwtTokenProvider tokens;
  private JwtAuthenticationGatewayFilter filter;

  @BeforeEach
  void setUp() {
    props =
        new CommonProperties(
            null,
            null,
            new CommonProperties.Jwt(
                SECRET,
                Duration.ofMinutes(5),
                Duration.ofDays(1),
                "tests",
                "Authorization",
                "Bearer "));
    tokens = new JwtTokenProvider(props);
    var jwtProps = new GatewayJwtProperties(true, List.of("/api/auth/**", "/actuator/**"));
    filter = new JwtAuthenticationGatewayFilter(props, jwtProps);
  }

  @Test
  void public_path_passes_through_without_token() {
    var ex = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/login").build());
    StepCapturingChain chain = new StepCapturingChain();
    filter.filter(ex, chain).block();
    assertThat(chain.invoked).isTrue();
  }

  @Test
  void missing_authorization_header_returns_401() {
    var ex = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders").build());
    StepCapturingChain chain = new StepCapturingChain();
    filter.filter(ex, chain).block();
    assertThat(chain.invoked).isFalse();
    assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(ex.getResponse().getHeaders().getFirst("X-Auth-Error")).isNotBlank();
  }

  @Test
  void valid_token_propagates_subject_and_roles_headers() {
    String jwt = tokens.generateAccessToken("alice", List.of("ROLE_ADMIN", "ROLE_USER"), Map.of());
    var ex =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/orders")
                .header("Authorization", "Bearer " + jwt)
                .build());
    StepCapturingChain chain = new StepCapturingChain();
    filter.filter(ex, chain).block();
    assertThat(chain.invoked).isTrue();
    assertThat(chain.lastSubject).isEqualTo("alice");
    assertThat(chain.lastRoles).contains("ROLE_ADMIN").contains("ROLE_USER");
  }

  @Test
  void tampered_token_returns_401() {
    String jwt = tokens.generateAccessToken("alice", List.of("ROLE_USER"), Map.of());
    var ex =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/orders")
                .header("Authorization", "Bearer " + jwt + "TAMPER")
                .build());
    StepCapturingChain chain = new StepCapturingChain();
    filter.filter(ex, chain).block();
    assertThat(chain.invoked).isFalse();
    assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void filter_runs_before_default_route_filters() {
    assertThat(filter.getOrder()).isLessThan(0);
  }

  private static final class StepCapturingChain implements GatewayFilterChain {
    boolean invoked;
    String lastSubject;
    String lastRoles;

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange) {
      invoked = true;
      lastSubject = exchange.getRequest().getHeaders().getFirst("X-Auth-Subject");
      lastRoles = exchange.getRequest().getHeaders().getFirst("X-Auth-Roles");
      return Mono.empty();
    }
  }
}
