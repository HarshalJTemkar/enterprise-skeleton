package com.enterprise.auth.demo;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Demonstrates {@code @Scheduled} jobs running on the shared
 * {@code taskScheduler} registered by {@code SchedulerAutoConfiguration}.
 *
 * <p>Each job is independently configurable and can be disabled by setting
 * {@code demo.scheduler.enabled=false} without restarting the scheduler
 * infrastructure.</p>
 */
@Component
@ConditionalOnProperty(prefix = "demo.scheduler", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class DemoScheduler {

    private static final Logger log = LoggerFactory.getLogger(DemoScheduler.class);
    private final AtomicLong fixedCounter = new AtomicLong();
    private final AtomicLong cronCounter = new AtomicLong();

    /**
     * Fixed-delay job. The delay is externalized so it can be tuned per
     * environment without a rebuild.
     */
    @Scheduled(fixedDelayString = "${demo.scheduler.fixed-delay:PT30S}",
            initialDelayString = "${demo.scheduler.initial-delay:PT5S}")
    public void fixedDelayJob() {
        log.info("[fixedDelayJob] tick #{} @ {}", fixedCounter.incrementAndGet(), Instant.now());
    }

    /**
     * Cron job whose expression is fully externalized. Default: every minute.
     */
    @Scheduled(cron = "${demo.scheduler.cron:0 * * * * *}")
    public void cronJob() {
        log.info("[cronJob] tick #{} @ {}", cronCounter.incrementAndGet(), Instant.now());
    }
}
