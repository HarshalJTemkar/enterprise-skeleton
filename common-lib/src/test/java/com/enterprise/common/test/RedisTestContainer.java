package com.enterprise.common.test;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Reusable Redis Testcontainer used for idempotency / cache integration tests. Exposes port {@code
 * 6379} on a random host port.
 */
public class RedisTestContainer extends GenericContainer<RedisTestContainer> {

  private static final DockerImageName IMAGE = DockerImageName.parse("redis:7-alpine");
  private static RedisTestContainer INSTANCE;

  private RedisTestContainer() {
    super(IMAGE);
    withExposedPorts(6379);
    withReuse(true);
  }

  /** Shared singleton instance. */
  public static synchronized RedisTestContainer getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new RedisTestContainer();
      INSTANCE.start();
    }
    return INSTANCE;
  }

  /** Convenience: the host port mapped to Redis 6379. */
  public int getRedisPort() {
    return getMappedPort(6379);
  }

  @Override
  public void stop() {
    // singleton – do not stop between tests
  }
}
