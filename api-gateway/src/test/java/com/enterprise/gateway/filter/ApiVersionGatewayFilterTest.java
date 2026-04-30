package com.enterprise.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

@DisplayName("ApiVersionGatewayFilter Tests")
class ApiVersionGatewayFilterTest {

  private ApiVersionGatewayFilter filterFactory;

  @BeforeEach
  void setUp() {
    filterFactory = new ApiVersionGatewayFilter();
  }

  @Nested
  @DisplayName("Header-based Versioning")
  class HeaderBasedVersioning {

    @Test
    @DisplayName("Should extract version from X-API-Version header")
    void shouldExtractVersionFromHeader() {
      var config = new ApiVersionGatewayFilter.Config();
      config.setDefaultVersion("v1");
      config.setSupportedVersions(List.of("v1", "v2"));

      var filter = filterFactory.apply(config);
      var exchange =
          MockServerWebExchange.from(
              MockServerHttpRequest.get("/api/v1/sample/123")
                  .header("X-API-Version", "v2")
                  .build());

      var chain = new TestChain();
      filter.filter(exchange, chain).block();

      assertThat(chain.invoked).isTrue();
      assertThat(chain.resolvedVersion).isEqualTo("v2");
    }

    @Test
    @DisplayName("Should normalize version format (1 -> v1)")
    void shouldNormalizeVersionFormat() {
      var config = new ApiVersionGatewayFilter.Config();
      config.setDefaultVersion("v1");
      config.setSupportedVersions(List.of("v1"));

      var filter = filterFactory.apply(config);
      var exchange =
          MockServerWebExchange.from(
              MockServerHttpRequest.get("/api/v1/sample/123").header("X-API-Version", "1").build());

      var chain = new TestChain();
      filter.filter(exchange, chain).block();

      assertThat(chain.invoked).isTrue();
      assertThat(chain.resolvedVersion).isEqualTo("v1");
    }

    @Test
    @DisplayName("Should normalize version with minor (1.0 -> v1)")
    void shouldNormalizeVersionWithMinor() {
      var config = new ApiVersionGatewayFilter.Config();
      config.setDefaultVersion("v1");
      config.setSupportedVersions(List.of("v1"));

      var filter = filterFactory.apply(config);
      var exchange =
          MockServerWebExchange.from(
              MockServerHttpRequest.get("/api/v1/sample/123")
                  .header("X-API-Version", "1.0")
                  .build());

      var chain = new TestChain();
      filter.filter(exchange, chain).block();

      assertThat(chain.invoked).isTrue();
      assertThat(chain.resolvedVersion).isEqualTo("v1");
    }

    @Test
    @DisplayName("Should use default version when header missing")
    void shouldUseDefaultVersionWhenMissing() {
      var config = new ApiVersionGatewayFilter.Config();
      config.setDefaultVersion("v1");
      config.setSupportedVersions(List.of("v1", "v2"));

      var filter = filterFactory.apply(config);
      var exchange =
          MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/sample/123").build());

      var chain = new TestChain();
      filter.filter(exchange, chain).block();

      assertThat(chain.invoked).isTrue();
      assertThat(chain.resolvedVersion).isEqualTo("v1");
    }
  }

  @Nested
  @DisplayName("Version Validation")
  class VersionValidation {

    @Test
    @DisplayName("Should reject unsupported version when validation enabled")
    void shouldRejectUnsupportedVersion() {
      var config = new ApiVersionGatewayFilter.Config();
      config.setDefaultVersion("v1");
      config.setSupportedVersions(List.of("v1", "v2"));
      config.setRequired(false);

      var filter = filterFactory.apply(config);
      var exchange =
          MockServerWebExchange.from(
              MockServerHttpRequest.get("/api/v1/sample/123")
                  .header("X-API-Version", "v99")
                  .build());

      var chain = new TestChain();
      filter.filter(exchange, chain).block();

      assertThat(chain.invoked).isFalse();
      assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(exchange.getResponse().getHeaders().getFirst("X-Version-Error"))
          .contains("Unsupported API version");
    }

    @Test
    @DisplayName("Should require version when configured as required")
    void shouldRequireVersionWhenConfigured() {
      var config = new ApiVersionGatewayFilter.Config();
      config.setRequired(true);
      config.setSupportedVersions(List.of("v1"));

      var filter = filterFactory.apply(config);
      var exchange =
          MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/sample/123").build());

      var chain = new TestChain();
      filter.filter(exchange, chain).block();

      assertThat(chain.invoked).isFalse();
      assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(exchange.getResponse().getHeaders().getFirst("X-Version-Error"))
          .contains("API version is required");
    }

    @Test
    @DisplayName("Should accept supported version")
    void shouldAcceptSupportedVersion() {
      var config = new ApiVersionGatewayFilter.Config();
      config.setDefaultVersion("v1");
      config.setSupportedVersions(List.of("v1", "v2", "v3"));

      var filter = filterFactory.apply(config);
      var exchange =
          MockServerWebExchange.from(
              MockServerHttpRequest.get("/api/v1/sample/123")
                  .header("X-API-Version", "v2")
                  .build());

      var chain = new TestChain();
      filter.filter(exchange, chain).block();

      assertThat(chain.invoked).isTrue();
      assertThat(chain.resolvedVersion).isEqualTo("v2");
    }
  }

  @Nested
  @DisplayName("Vendor Media Type Versioning")
  class VendorMediaTypeVersioning {

    @Test
    @DisplayName("Should extract version from Accept header with vendor media type")
    void shouldExtractFromVendorMediaType() {
      var config = new ApiVersionGatewayFilter.Config();
      config.setDefaultVersion("v1");
      config.setSupportedVersions(List.of("v1", "v2"));
      config.setEnableVendorMediaType(true);
      config.setVendorPrefix("enterprise");

      var filter = filterFactory.apply(config);
      var exchange =
          MockServerWebExchange.from(
              MockServerHttpRequest.get("/api/v1/sample/123")
                  .header(HttpHeaders.ACCEPT, "application/vnd.enterprise.v2+json")
                  .build());

      var chain = new TestChain();
      filter.filter(exchange, chain).block();

      assertThat(chain.invoked).isTrue();
      assertThat(chain.resolvedVersion).isEqualTo("v2");
    }

    @Test
    @DisplayName("Should prefer X-API-Version header over Accept header")
    void shouldPreferHeaderOverMediaType() {
      var config = new ApiVersionGatewayFilter.Config();
      config.setDefaultVersion("v1");
      config.setSupportedVersions(List.of("v1", "v2"));
      config.setEnableVendorMediaType(true);
      config.setVendorPrefix("enterprise");

      var filter = filterFactory.apply(config);
      var exchange =
          MockServerWebExchange.from(
              MockServerHttpRequest.get("/api/v1/sample/123")
                  .header("X-API-Version", "v2")
                  .header(HttpHeaders.ACCEPT, "application/vnd.enterprise.v1+json")
                  .build());

      var chain = new TestChain();
      filter.filter(exchange, chain).block();

      assertThat(chain.invoked).isTrue();
      assertThat(chain.resolvedVersion).isEqualTo("v2");
    }

    @Test
    @DisplayName("Should not extract version from media type when disabled")
    void shouldNotExtractWhenDisabled() {
      var config = new ApiVersionGatewayFilter.Config();
      config.setDefaultVersion("v1");
      config.setSupportedVersions(List.of("v1", "v2"));
      config.setEnableVendorMediaType(false);

      var filter = filterFactory.apply(config);
      var exchange =
          MockServerWebExchange.from(
              MockServerHttpRequest.get("/api/v1/sample/123")
                  .header(HttpHeaders.ACCEPT, "application/vnd.enterprise.v2+json")
                  .build());

      var chain = new TestChain();
      filter.filter(exchange, chain).block();

      assertThat(chain.invoked).isTrue();
      // Should use default since media type parsing is disabled
      assertThat(chain.resolvedVersion).isEqualTo("v1");
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCases {

    @Test
    @DisplayName("Should handle empty version header")
    void shouldHandleEmptyVersionHeader() {
      var config = new ApiVersionGatewayFilter.Config();
      config.setDefaultVersion("v1");

      var filter = filterFactory.apply(config);
      var exchange =
          MockServerWebExchange.from(
              MockServerHttpRequest.get("/api/v1/sample/123").header("X-API-Version", "").build());

      var chain = new TestChain();
      filter.filter(exchange, chain).block();

      assertThat(chain.invoked).isTrue();
      assertThat(chain.resolvedVersion).isEqualTo("v1");
    }

    @Test
    @DisplayName("Should handle whitespace in version header")
    void shouldHandleWhitespaceInVersion() {
      var config = new ApiVersionGatewayFilter.Config();
      config.setDefaultVersion("v1");
      config.setSupportedVersions(List.of("v1", "v2"));

      var filter = filterFactory.apply(config);
      var exchange =
          MockServerWebExchange.from(
              MockServerHttpRequest.get("/api/v1/sample/123")
                  .header("X-API-Version", "  v2  ")
                  .build());

      var chain = new TestChain();
      filter.filter(exchange, chain).block();

      assertThat(chain.invoked).isTrue();
      assertThat(chain.resolvedVersion).isEqualTo("v2");
    }

    @Test
    @DisplayName("Should handle case-insensitive version")
    void shouldHandleCaseInsensitiveVersion() {
      var config = new ApiVersionGatewayFilter.Config();
      config.setDefaultVersion("v1");
      config.setSupportedVersions(List.of("v1", "v2"));

      var filter = filterFactory.apply(config);
      var exchange =
          MockServerWebExchange.from(
              MockServerHttpRequest.get("/api/v1/sample/123")
                  .header("X-API-Version", "V2")
                  .build());

      var chain = new TestChain();
      filter.filter(exchange, chain).block();

      assertThat(chain.invoked).isTrue();
      assertThat(chain.resolvedVersion).isEqualTo("v2");
    }
  }

  private static class TestChain implements GatewayFilterChain {
    boolean invoked = false;
    String resolvedVersion = null;

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange) {
      invoked = true;
      resolvedVersion = exchange.getRequest().getHeaders().getFirst("X-Resolved-API-Version");
      return Mono.empty();
    }
  }
}
