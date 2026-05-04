package com.enterprise.common.idempotency;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Registers the idempotency store + aspect. Active only when:
 *
 * <ul>
 *   <li>{@code feature.idempotency.enabled=true}
 *   <li>{@link StringRedisTemplate} and AspectJ are on the classpath
 * </ul>
 */
@Configuration
@ConditionalOnClass({Aspect.class, StringRedisTemplate.class})
@ConditionalOnProperty(
    prefix = "feature.idempotency",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyAutoConfiguration {

  /** Redis-backed default implementation. */
  @Bean
  @ConditionalOnMissingBean
  public IdempotencyStore idempotencyStore(StringRedisTemplate redis, IdempotencyProperties props) {
    return new RedisIdempotencyStore(redis, props.getKeyPrefix());
  }

  /** AOP aspect wrapping every {@code @Idempotent} method. */
  @Bean
  @ConditionalOnMissingBean
  public IdempotencyAspect idempotencyAspect(IdempotencyStore store, IdempotencyProperties props) {
    return new IdempotencyAspect(store, props);
  }
}
