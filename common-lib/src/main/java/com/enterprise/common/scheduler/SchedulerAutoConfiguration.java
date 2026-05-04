package com.enterprise.common.scheduler;

import java.util.concurrent.Executors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Enables {@code @Scheduled} support and publishes a {@link TaskScheduler} bean fed from {@link
 * SchedulerProperties}.
 *
 * <p>Any service annotated with {@code @Scheduled(cron = "...")} or
 * {@code @Scheduled(fixedDelayString = "${my.job.delay}")} will be picked up automatically. Disable
 * globally with {@code enterprise.common.scheduler.enabled=false}.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    prefix = "enterprise.common.scheduler",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(SchedulerProperties.class)
public class SchedulerAutoConfiguration {

  /**
   * Builds the shared {@link TaskScheduler}. A virtual-thread-per-task scheduler is used when
   * {@link SchedulerProperties#isUseVirtualThreads()} is {@code true}, otherwise a bounded {@link
   * ThreadPoolTaskScheduler}.
   *
   * @param props externalized scheduler configuration
   * @return the configured scheduler bean
   */
  @Bean(name = "taskScheduler")
  @ConditionalOnMissingBean(name = "taskScheduler")
  public TaskScheduler taskScheduler(SchedulerProperties props) {
    if (props.isUseVirtualThreads()) {
      // Uses a single-thread scheduled executor whose fires dispatch
      // work onto virtual threads so many @Scheduled jobs can run
      // concurrently without blocking each other.
      var delegate =
          Executors.newScheduledThreadPool(
              1, Thread.ofVirtual().name(props.getThreadNamePrefix(), 0L).factory());
      return new ConcurrentTaskScheduler(delegate);
    }
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(props.getPoolSize());
    scheduler.setThreadNamePrefix(props.getThreadNamePrefix());
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(props.getAwaitTerminationSeconds());
    scheduler.setRemoveOnCancelPolicy(true);
    scheduler.initialize();
    return scheduler;
  }
}
