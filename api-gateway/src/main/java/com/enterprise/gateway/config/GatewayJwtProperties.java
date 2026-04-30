package com.enterprise.gateway.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.security.jwt")
public record GatewayJwtProperties(boolean enabled, List<String> publicPaths) {
  public GatewayJwtProperties {
    if (publicPaths == null) publicPaths = List.of();
  }
}
