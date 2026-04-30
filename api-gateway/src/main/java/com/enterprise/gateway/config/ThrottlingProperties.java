package com.enterprise.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for gateway throttling policies.
 *
 * <p>Defines tiered rate limiting strategies for different endpoint categories. Used in conjunction
 * with Spring Cloud Gateway's RequestRateLimiter filter.
 */
@ConfigurationProperties(prefix = "gateway.throttling")
public record ThrottlingProperties(
    Tier critical, Tier sensitive, Tier standard, Tier graphql, Tier publicApi) {
  public ThrottlingProperties {
    if (critical == null) critical = new Tier(5, 10);
    if (sensitive == null) sensitive = new Tier(30, 50);
    if (standard == null) standard = new Tier(100, 200);
    if (graphql == null) graphql = new Tier(20, 40);
    if (publicApi == null) publicApi = new Tier(50, 100);
  }

  /**
   * Rate limiting tier configuration.
   *
   * @param replenishRate Number of tokens added per second (requests/sec)
   * @param burstCapacity Maximum burst capacity (max concurrent requests)
   */
  public record Tier(int replenishRate, int burstCapacity) {
    public Tier {
      if (replenishRate <= 0) throw new IllegalArgumentException("replenishRate must be positive");
      if (burstCapacity <= 0) throw new IllegalArgumentException("burstCapacity must be positive");
      if (burstCapacity < replenishRate) {
        throw new IllegalArgumentException("burstCapacity should be >= replenishRate");
      }
    }
  }
}
