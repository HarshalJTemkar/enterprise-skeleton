package com.enterprise.common.async;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Registers:
 * <ul>
 *   <li>{@code taskExecutor} – the default {@code @Async} executor</li>
 *   <li>{@code enterpriseParallelExecutor} – dedicated executor for
 *       {@link ParallelExecutor} fan-out tasks</li>
 *   <li>{@link ParallelExecutor} – a ready-to-inject helper bean</li>
 * </ul>
 *
 * <p>Enables {@code @Async} on the whole application via
 * {@link EnableAsync}. Toggle the whole subsystem with
 * {@code enterprise.common.async.enabled=false}.</p>
 */
@Configuration
@EnableAsync
@ConditionalOnProperty(prefix = "enterprise.common.async", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AsyncProperties.class)
public class AsyncAutoConfiguration implements AsyncConfigurer {

    private final AsyncProperties props;

    public AsyncAutoConfiguration(AsyncProperties props) {
        this.props = props;
    }

    /**
     * Primary {@link Executor} used by {@code @Async} methods without an
     * explicit qualifier.
     *
     * <p>When {@code useVirtualThreads=true} (default on JDK 21+), an
     * unbounded virtual-thread-per-task executor is returned. Otherwise a
     * bounded {@link ThreadPoolTaskExecutor} is built from the pool sizes.</p>
     */
    @Override
    @Bean(name = "taskExecutor", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        if (props.isUseVirtualThreads()) {
            return Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                    .name(props.getThreadNamePrefix(), 0L).factory());
        }
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(props.getCorePoolSize());
        exec.setMaxPoolSize(props.getMaxPoolSize());
        exec.setQueueCapacity(props.getQueueCapacity());
        exec.setThreadNamePrefix(props.getThreadNamePrefix());
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(30);
        exec.initialize();
        return exec;
    }

    /**
     * Handler that logs uncaught exceptions thrown by {@code void @Async}
     * methods. Override with your own bean to customize.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    /**
     * Dedicated executor used by {@link ParallelExecutor}. Kept separate from
     * {@code taskExecutor} so high-volume parallel fan-out cannot starve
     * general-purpose {@code @Async} traffic.
     */
    @Bean(name = "enterpriseParallelExecutor", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "enterpriseParallelExecutor")
    @ConditionalOnProperty(prefix = "enterprise.common.async.parallel",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    public Executor enterpriseParallelExecutor() {
        AsyncProperties.Parallel p = props.getParallel();
        if (p.isUseVirtualThreads()) {
            return Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                    .name(p.getThreadNamePrefix(), 0L).factory());
        }
        return Executors.newFixedThreadPool(p.getPoolSize(),
                r -> {
                    Thread t = new Thread(r);
                    t.setName(p.getThreadNamePrefix() + t.getId());
                    t.setDaemon(true);
                    return t;
                });
    }

    /**
     * The user-facing {@link ParallelExecutor} bean for concurrent fan-out.
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "enterprise.common.async.parallel",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    public ParallelExecutor parallelExecutor() {
        return new ParallelExecutor(enterpriseParallelExecutor());
    }
}
