package com.enterprise.common.cache;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code enterprise.common.cache.*}. */
@ConfigurationProperties(prefix = "enterprise.common.cache")
public class CacheProperties {

    /** Master switch. */
    private boolean enabled = true;

    /** Backend: {@code caffeine} (in-memory, default) or {@code redis}. */
    private String type = "caffeine";

    /** Pre-create these caches at startup so first-touch is fast. */
    private List<String> names = List.of();

    private final Caffeine caffeine = new Caffeine();
    private final Redis redis = new Redis();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public List<String> getNames() { return names; }
    public void setNames(List<String> v) { this.names = v; }
    public Caffeine getCaffeine() { return caffeine; }
    public Redis getRedis() { return redis; }

    public static class Caffeine {
        /** Max entries per cache. */
        private long maximumSize = 10_000L;
        /** Per-entry TTL after write. */
        private Duration expireAfterWrite = Duration.ofMinutes(10);
        /** Record hit/miss stats (small overhead). */
        private boolean recordStats = true;

        public long getMaximumSize() { return maximumSize; }
        public void setMaximumSize(long v) { this.maximumSize = v; }
        public Duration getExpireAfterWrite() { return expireAfterWrite; }
        public void setExpireAfterWrite(Duration v) { this.expireAfterWrite = v; }
        public boolean isRecordStats() { return recordStats; }
        public void setRecordStats(boolean v) { this.recordStats = v; }
    }

    public static class Redis {
        /** TTL applied to entries unless overridden per cache. */
        private Duration timeToLive = Duration.ofMinutes(10);
        /** Cache-wide key prefix. */
        private String keyPrefix = "cache:";
        /** Skip caching {@code null} values to save round-trips. */
        private boolean cacheNullValues = false;

        public Duration getTimeToLive() { return timeToLive; }
        public void setTimeToLive(Duration v) { this.timeToLive = v; }
        public String getKeyPrefix() { return keyPrefix; }
        public void setKeyPrefix(String v) { this.keyPrefix = v; }
        public boolean isCacheNullValues() { return cacheNullValues; }
        public void setCacheNullValues(boolean v) { this.cacheNullValues = v; }
    }
}
