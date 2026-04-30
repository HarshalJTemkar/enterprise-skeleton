package com.enterprise.common.async;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Helper for running a batch of independent tasks <em>concurrently</em> and
 * waiting on the aggregate result. Built on top of the
 * {@code enterpriseParallelExecutor} bean registered by
 * {@link AsyncAutoConfiguration}.
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * List<Invoice> invoices = parallelExecutor.map(orderIds, orderId ->
 *         billingClient.fetchInvoice(orderId));
 * }</pre>
 */
public class ParallelExecutor {

    private static final Logger log = LoggerFactory.getLogger(ParallelExecutor.class);

    private final Executor executor;

    /**
     * Constructs a new {@code ParallelExecutor} bound to the shared parallel
     * executor bean.
     *
     * @param executor the executor created by {@link AsyncAutoConfiguration}
     */
    public ParallelExecutor(@Qualifier("enterpriseParallelExecutor") Executor executor) {
        this.executor = executor;
    }

    /**
     * Applies {@code mapper} to every item in {@code inputs} concurrently and
     * returns the results in the <em>same order</em> as the input.
     *
     * @param inputs input collection (may be {@code null} or empty)
     * @param mapper conversion function; exceptions propagate via the returned
     *               {@link java.util.concurrent.CompletionException}
     * @param <I>    input element type
     * @param <O>    output element type
     * @return ordered list of mapped results
     */
    public <I, O> List<O> map(Collection<I> inputs, Function<I, O> mapper) {
        if (inputs == null || inputs.isEmpty()) return List.of();
        List<CompletableFuture<O>> futures = inputs.stream()
                .map(in -> CompletableFuture.supplyAsync(() -> mapper.apply(in), executor))
                .toList();
        return joinAll(futures);
    }

    /**
     * Like {@link #map(Collection, Function)} but with a hard timeout for the
     * whole batch. Tasks that have not finished are cancelled.
     *
     * @throws java.util.concurrent.CompletionException wrapping a
     *         {@link TimeoutException} if the aggregate exceeds {@code timeoutMs}
     */
    public <I, O> List<O> map(Collection<I> inputs, Function<I, O> mapper, long timeoutMs) {
        if (inputs == null || inputs.isEmpty()) return List.of();
        List<CompletableFuture<O>> futures = inputs.stream()
                .map(in -> CompletableFuture.supplyAsync(() -> mapper.apply(in), executor))
                .toList();
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        try {
            all.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            log.warn("Parallel batch exceeded {} ms – cancelling {} tasks", timeoutMs, futures.size());
            futures.forEach(f -> f.cancel(true));
            throw new java.util.concurrent.CompletionException(te);
        } catch (Exception e) {
            throw new java.util.concurrent.CompletionException(e);
        }
        return futures.stream().map(CompletableFuture::join).toList();
    }

    /**
     * Launches every {@link Runnable} concurrently and blocks until they all
     * complete. Useful for "fire N writes in parallel" type workflows.
     */
    public void runAll(Collection<Runnable> tasks) {
        if (tasks == null || tasks.isEmpty()) return;
        List<CompletableFuture<Void>> futures = new ArrayList<>(tasks.size());
        for (Runnable r : tasks) futures.add(CompletableFuture.runAsync(r, executor));
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    /**
     * Joins a list of futures, preserving order and rethrowing any failure.
     */
    private static <T> List<T> joinAll(List<CompletableFuture<T>> futures) {
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    /**
     * Expose the backing executor for callers that want to build their own
     * {@link CompletableFuture} pipelines against the shared pool.
     */
    public Executor executor() {
        return executor;
    }

    /**
     * Gracefully shuts down the underlying executor if it is an
     * {@link ExecutorService} (called by the Spring context on destroy).
     */
    public void shutdown() {
        if (executor instanceof ExecutorService es) {
            es.shutdown();
        }
    }
}
