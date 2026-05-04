package com.enterprise.common.async;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code enterprise.common.async.*}. Controls both the primary {@code @Async} executor and
 * the dedicated parallel executor used by {@link ParallelExecutor}.
 */
@ConfigurationProperties(prefix = "enterprise.common.async")
public class AsyncProperties {

  /** Master switch for async auto-configuration. */
  private boolean enabled = true;

  /**
   * When {@code true}, uses JDK 21 virtual threads for {@code @Async} (unbounded, cheap). When
   * {@code false} a platform-thread pool is used with the {@code core/max/queue} sizes below.
   */
  private boolean useVirtualThreads = true;

  /** Core pool size (platform-thread mode only). */
  private int corePoolSize = 8;

  /** Max pool size (platform-thread mode only). */
  private int maxPoolSize = 32;

  /** Bounded queue capacity (platform-thread mode only). */
  private int queueCapacity = 500;

  /** Thread-name prefix for async workers. */
  private String threadNamePrefix = "enterprise-async-";

  private final Parallel parallel = new Parallel();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isUseVirtualThreads() {
    return useVirtualThreads;
  }

  public void setUseVirtualThreads(boolean v) {
    this.useVirtualThreads = v;
  }

  public int getCorePoolSize() {
    return corePoolSize;
  }

  public void setCorePoolSize(int v) {
    this.corePoolSize = v;
  }

  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  public void setMaxPoolSize(int v) {
    this.maxPoolSize = v;
  }

  public int getQueueCapacity() {
    return queueCapacity;
  }

  public void setQueueCapacity(int v) {
    this.queueCapacity = v;
  }

  public String getThreadNamePrefix() {
    return threadNamePrefix;
  }

  public void setThreadNamePrefix(String v) {
    this.threadNamePrefix = v;
  }

  public Parallel getParallel() {
    return parallel;
  }

  /** Settings for the ParallelExecutor. */
  public static class Parallel {
    /** Individual feature switch. */
    private boolean enabled = true;

    /** Whether to use virtual threads for fan-out tasks. */
    private boolean useVirtualThreads = true;

    /** Pool size when using platform threads. */
    private int poolSize = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);

    /** Thread-name prefix. */
    private String threadNamePrefix = "enterprise-parallel-";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean isUseVirtualThreads() {
      return useVirtualThreads;
    }

    public void setUseVirtualThreads(boolean v) {
      this.useVirtualThreads = v;
    }

    public int getPoolSize() {
      return poolSize;
    }

    public void setPoolSize(int v) {
      this.poolSize = v;
    }

    public String getThreadNamePrefix() {
      return threadNamePrefix;
    }

    public void setThreadNamePrefix(String v) {
      this.threadNamePrefix = v;
    }
  }
}
