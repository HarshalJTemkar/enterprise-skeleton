package com.enterprise.common.idempotency;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code feature.idempotency.*}. */
@ConfigurationProperties(prefix = "feature.idempotency")
public class IdempotencyProperties {

  /** Master switch. */
  private boolean enabled = true;

  /** HTTP header that carries the client-supplied idempotency key. */
  private String headerName = "Idempotency-Key";

  /** Default cache TTL when {@link Idempotent#ttl()} is blank. */
  private Duration ttl = Duration.ofMinutes(10);

  /** Prefix applied to every Redis key written by the store. */
  private String keyPrefix = "idempotency:";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean v) {
    this.enabled = v;
  }

  public String getHeaderName() {
    return headerName;
  }

  public void setHeaderName(String v) {
    this.headerName = v;
  }

  public Duration getTtl() {
    return ttl;
  }

  public void setTtl(Duration v) {
    this.ttl = v;
  }

  public String getKeyPrefix() {
    return keyPrefix;
  }

  public void setKeyPrefix(String v) {
    this.keyPrefix = v;
  }
}
