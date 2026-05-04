package com.enterprise.common.idempotency;

import java.time.Duration;
import java.util.Optional;

/**
 * Abstraction over the idempotency cache. The default implementation is Redis-backed; swap it in
 * tests or non-Redis environments by supplying a custom bean.
 */
public interface IdempotencyStore {

  /**
   * Atomically reserve the key. Returns {@link Optional#empty()} when the reservation succeeded
   * (caller should execute the operation), or the previously cached value when the key already
   * existed.
   */
  Optional<String> putIfAbsent(String key, String responseJson, Duration ttl);

  /** Lookup a cached response without modifying the store. */
  Optional<String> find(String key);

  /** Manually delete an entry – useful when the caller wants to retry. */
  void invalidate(String key);
}
