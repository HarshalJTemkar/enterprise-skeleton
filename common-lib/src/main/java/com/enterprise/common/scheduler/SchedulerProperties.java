package com.enterprise.common.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code enterprise.common.scheduler.*}. Controls the shared
 * {@link org.springframework.scheduling.TaskScheduler} used for
 * {@code @Scheduled} methods.
 */
@ConfigurationProperties(prefix = "enterprise.common.scheduler")
public class SchedulerProperties {

    /** Master switch for scheduler auto-configuration. */
    private boolean enabled = true;

    /**
     * Use virtual threads (JDK 21) for scheduled task execution. Virtual
     * threads are a good fit here because scheduled jobs are usually I/O
     * bound and infrequent.
     */
    private boolean useVirtualThreads = true;

    /** Pool size when {@link #useVirtualThreads} is {@code false}. */
    private int poolSize = 5;

    /** Thread-name prefix for scheduler threads. */
    private String threadNamePrefix = "enterprise-scheduler-";

    /** Await-termination seconds on context shutdown. */
    private int awaitTerminationSeconds = 20;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isUseVirtualThreads() { return useVirtualThreads; }
    public void setUseVirtualThreads(boolean v) { this.useVirtualThreads = v; }
    public int getPoolSize() { return poolSize; }
    public void setPoolSize(int v) { this.poolSize = v; }
    public String getThreadNamePrefix() { return threadNamePrefix; }
    public void setThreadNamePrefix(String v) { this.threadNamePrefix = v; }
    public int getAwaitTerminationSeconds() { return awaitTerminationSeconds; }
    public void setAwaitTerminationSeconds(int v) { this.awaitTerminationSeconds = v; }
}
