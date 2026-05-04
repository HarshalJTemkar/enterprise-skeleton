package com.enterprise.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache abstraction wired up from {@link CacheProperties}.
 *
 * <p>Two backends are supported and selected with {@code enterprise.common.cache.type}:
 *
 * <ul>
 *   <li>{@code caffeine} (default) – in-process, very fast, per-instance.
 *   <li>{@code redis} – distributed (see nested {@link RedisCache} class).
 * </ul>
 *
 * <p>Disable the whole feature with {@code enterprise.common.cache.enabled=false}.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(
    prefix = "enterprise.common.cache",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

  /** Caffeine-backed in-memory cache manager (default). */
  @Bean
  @ConditionalOnClass(Caffeine.class)
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "enterprise.common.cache",
      name = "type",
      havingValue = "caffeine",
      matchIfMissing = true)
  public CacheManager caffeineCacheManager(CacheProperties props) {
    var c = props.getCaffeine();
    Caffeine<Object, Object> spec =
        Caffeine.newBuilder()
            .maximumSize(c.getMaximumSize())
            .expireAfterWrite(c.getExpireAfterWrite().toMillis(), TimeUnit.MILLISECONDS);
    if (c.isRecordStats()) spec.recordStats();

    CaffeineCacheManager mgr = new CaffeineCacheManager();
    mgr.setCaffeine(spec);
    if (!props.getNames().isEmpty()) {
      mgr.setCacheNames(props.getNames());
    }
    return mgr;
  }

  /**
   * Redis-backed cache configuration. Loaded as a nested {@link Configuration} so the parent class
   * never references Redis types directly — keeps class-loading clean when Redis is absent.
   */
  @Configuration
  @ConditionalOnClass(
      name = {
        "org.springframework.data.redis.cache.RedisCacheManager",
        "org.springframework.data.redis.serializer.RedisSerializer"
      })
  @ConditionalOnProperty(prefix = "enterprise.common.cache", name = "type", havingValue = "redis")
  static class RedisCache {

    /** Redis-backed distributed cache manager. */
    @Bean
    @ConditionalOnMissingBean
    public CacheManager redisCacheManager(
        org.springframework.data.redis.connection.RedisConnectionFactory cf,
        CacheProperties props) {
      var r = props.getRedis();
      org.springframework.data.redis.cache.RedisCacheConfiguration baseCfg =
          org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
              .entryTtl(r.getTimeToLive())
              .prefixCacheNameWith(r.getKeyPrefix())
              .serializeKeysWith(
                  org.springframework.data.redis.serializer.RedisSerializationContext
                      .SerializationPair.fromSerializer(
                      new org.springframework.data.redis.serializer.StringRedisSerializer()))
              .serializeValuesWith(
                  org.springframework.data.redis.serializer.RedisSerializationContext
                      .SerializationPair.fromSerializer(
                      new org.springframework.data.redis.serializer
                          .GenericJackson2JsonRedisSerializer()));
      final var cfg = r.isCacheNullValues() ? baseCfg : baseCfg.disableCachingNullValues();

      var builder =
          org.springframework.data.redis.cache.RedisCacheManager.builder(cf).cacheDefaults(cfg);
      if (!props.getNames().isEmpty()) {
        props.getNames().forEach(name -> builder.withCacheConfiguration(name, cfg));
      }
      return builder.build();
    }
  }
}
