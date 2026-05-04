package com.enterprise.common.graphql;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for the platform's GraphQL auto-configuration.
 *
 * <pre>
 * enterprise:
 *   common:
 *     graphql:
 *       enabled: true
 *       max-query-depth: 15
 *       max-query-complexity: 200
 *       introspection:
 *         enabled: false        # disable in production
 *       scalars:
 *         enabled: true         # registers DateTime, UUID, JSON, Long, BigDecimal
 *       error-mapping:
 *         enabled: true         # map ErrorCode → GraphQL extensions.errorCode
 *         expose-message: true  # surface localized message to client
 * </pre>
 */
@ConfigurationProperties(prefix = "enterprise.common.graphql")
public class GraphQlProperties {

  /** Master toggle for everything below. */
  private boolean enabled = true;

  /** Reject queries deeper than this many nested selections. {@code -1} disables. */
  private int maxQueryDepth = 15;

  /**
   * Reject queries whose computed complexity score exceeds this threshold. Each field counts as 1
   * by default. {@code -1} disables.
   */
  private int maxQueryComplexity = 200;

  private final Introspection introspection = new Introspection();
  private final Scalars scalars = new Scalars();
  private final ErrorMapping errorMapping = new ErrorMapping();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean v) {
    this.enabled = v;
  }

  public int getMaxQueryDepth() {
    return maxQueryDepth;
  }

  public void setMaxQueryDepth(int v) {
    this.maxQueryDepth = v;
  }

  public int getMaxQueryComplexity() {
    return maxQueryComplexity;
  }

  public void setMaxQueryComplexity(int v) {
    this.maxQueryComplexity = v;
  }

  public Introspection getIntrospection() {
    return introspection;
  }

  public Scalars getScalars() {
    return scalars;
  }

  public ErrorMapping getErrorMapping() {
    return errorMapping;
  }

  /** Schema introspection toggle (recommend false in production). */
  public static class Introspection {
    private boolean enabled = false;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean v) {
      this.enabled = v;
    }
  }

  /** Custom scalar registration toggle. */
  public static class Scalars {
    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean v) {
      this.enabled = v;
    }
  }

  /** Maps platform exceptions to GraphQL errors with stable {@code errorCode}. */
  public static class ErrorMapping {
    private boolean enabled = true;
    private boolean exposeMessage = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean v) {
      this.enabled = v;
    }

    public boolean isExposeMessage() {
      return exposeMessage;
    }

    public void setExposeMessage(boolean v) {
      this.exposeMessage = v;
    }
  }
}
