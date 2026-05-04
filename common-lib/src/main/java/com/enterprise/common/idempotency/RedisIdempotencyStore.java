package com.enterprise.common.idempotency;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis-backed {@link IdempotencyStore}. Uses {@code SET NX EX} semantics so the "reserve" and
 * "persist" steps are a single atomic operation.
 */
public class RedisIdempotencyStore implements IdempotencyStore {

  private final StringRedisTemplate redis;
  private final String prefix;

  public RedisIdempotencyStore(StringRedisTemplate redis, String prefix) {
    this.redis = redis;
    this.prefix = prefix == null ? "" : prefix;
  }

  @Override
  public Optional<String> putIfAbsent(String key, String responseJson, Duration ttl) {
    String k = prefix + key;
    Boolean ok = redis.opsForValue().setIfAbsent(k, responseJson, ttl);
    if (Boolean.TRUE.equals(ok)) {
      return Optional.empty(); // reservation made – caller should execute
    }
    return Optional.ofNullable(redis.opsForValue().get(k));
  }

  @Override
  public Optional<String> find(String key) {
    return Optional.ofNullable(redis.opsForValue().get(prefix + key));
  }

  @Override
  public void invalidate(String key) {
    redis.delete(prefix + key);
  }
}
