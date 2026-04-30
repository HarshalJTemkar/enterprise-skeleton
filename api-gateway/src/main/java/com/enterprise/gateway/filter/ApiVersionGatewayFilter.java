package com.enterprise.gateway.filter;

import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

/**
 * API versioning filter that extracts version from headers and validates it.
 *
 * <p>Supports multiple version sources in order of precedence:
 *
 * <ol>
 *   <li>X-API-Version header (e.g., "v1", "1", "1.0")
 *   <li>Accept header with vendor-specific media type (e.g., "application/vnd.enterprise.v1+json")
 *   <li>URL path version segment (already handled by routing predicates)
 * </ol>
 *
 * <p>Forwards the resolved version to downstream services via X-Resolved-API-Version header.
 */
@Slf4j
@Component
public class ApiVersionGatewayFilter
    extends AbstractGatewayFilterFactory<ApiVersionGatewayFilter.Config> {

  private static final String VERSION_HEADER = "X-API-Version";
  private static final String RESOLVED_VERSION_HEADER = "X-Resolved-API-Version";
  private static final String ACCEPT_HEADER = "Accept";

  public ApiVersionGatewayFilter() {
    super(Config.class);
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      ServerHttpRequest request = exchange.getRequest();

      String version = extractVersion(request, config);

      if (version == null && config.isRequired()) {
        log.warn("API version required but not provided for path: {}", request.getURI().getPath());
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().add("X-Version-Error", "API version is required");
        return response.setComplete();
      }

      if (version != null
          && config.getSupportedVersions() != null
          && !config.getSupportedVersions().isEmpty()) {
        if (!isVersionSupported(version, config.getSupportedVersions())) {
          log.warn(
              "Unsupported API version '{}' for path: {}", version, request.getURI().getPath());
          ServerHttpResponse response = exchange.getResponse();
          response.setStatusCode(HttpStatus.BAD_REQUEST);
          response
              .getHeaders()
              .add(
                  "X-Version-Error",
                  "Unsupported API version. Supported: "
                      + String.join(", ", config.getSupportedVersions()));
          return response.setComplete();
        }
      }

      // Use default version if none provided
      String resolvedVersion = version != null ? version : config.getDefaultVersion();

      if (resolvedVersion != null) {
        ServerHttpRequest mutated =
            request
                .mutate()
                .header(RESOLVED_VERSION_HEADER, normalizeVersion(resolvedVersion))
                .build();
        log.debug(
            "Resolved API version '{}' for path: {}", resolvedVersion, request.getURI().getPath());
        return chain.filter(exchange.mutate().request(mutated).build());
      }

      return chain.filter(exchange);
    };
  }

  private String extractVersion(ServerHttpRequest request, Config config) {
    // 1. Check X-API-Version header
    String headerVersion = request.getHeaders().getFirst(VERSION_HEADER);
    if (headerVersion != null && !headerVersion.isBlank()) {
      return headerVersion.trim();
    }

    // 2. Check Accept header for vendor media type (e.g., application/vnd.enterprise.v1+json)
    if (config.isEnableVendorMediaType()) {
      String accept = request.getHeaders().getFirst(ACCEPT_HEADER);
      if (accept != null) {
        String vendorVersion = extractVersionFromMediaType(accept, config.getVendorPrefix());
        if (vendorVersion != null) {
          return vendorVersion;
        }
      }
    }

    return null;
  }

  private String extractVersionFromMediaType(String accept, String vendorPrefix) {
    // Parse media types like: application/vnd.enterprise.v1+json
    if (accept.contains("vnd." + vendorPrefix)) {
      String[] parts = accept.split("[.+]");
      for (String part : parts) {
        if (part.startsWith("v") && part.length() > 1) {
          return part;
        }
      }
    }
    return null;
  }

  private boolean isVersionSupported(String version, List<String> supportedVersions) {
    String normalized = normalizeVersion(version);
    return supportedVersions.stream().anyMatch(v -> normalizeVersion(v).equals(normalized));
  }

  private String normalizeVersion(String version) {
    // Normalize "v1", "1", "1.0" to "v1"
    if (version == null) return null;
    version = version.trim().toLowerCase();
    if (!version.startsWith("v")) {
      version = "v" + version;
    }
    // Remove minor/patch versions for comparison (v1.0.0 -> v1)
    int dotIndex = version.indexOf('.');
    if (dotIndex > 0) {
      version = version.substring(0, dotIndex);
    }
    return version;
  }

  @Override
  public List<String> shortcutFieldOrder() {
    return Arrays.asList("defaultVersion", "supportedVersions", "required");
  }

  public static class Config {
    private String defaultVersion = "v1";
    private List<String> supportedVersions = List.of("v1");
    private boolean required = false;
    private boolean enableVendorMediaType = false;
    private String vendorPrefix = "enterprise";

    public String getDefaultVersion() {
      return defaultVersion;
    }

    public void setDefaultVersion(String defaultVersion) {
      this.defaultVersion = defaultVersion;
    }

    public List<String> getSupportedVersions() {
      return supportedVersions;
    }

    public void setSupportedVersions(List<String> supportedVersions) {
      this.supportedVersions = supportedVersions;
    }

    public boolean isRequired() {
      return required;
    }

    public void setRequired(boolean required) {
      this.required = required;
    }

    public boolean isEnableVendorMediaType() {
      return enableVendorMediaType;
    }

    public void setEnableVendorMediaType(boolean enableVendorMediaType) {
      this.enableVendorMediaType = enableVendorMediaType;
    }

    public String getVendorPrefix() {
      return vendorPrefix;
    }

    public void setVendorPrefix(String vendorPrefix) {
      this.vendorPrefix = vendorPrefix;
    }
  }
}
