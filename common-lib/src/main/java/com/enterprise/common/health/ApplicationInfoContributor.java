package com.enterprise.common.health;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Populates the {@code /actuator/info} endpoint with build and runtime metadata (Java version,
 * Spring Boot version, active profile, build version and git commit when available).
 *
 * <p>Build metadata is read from properties that can be set at package time:
 *
 * <ul>
 *   <li>{@code build.version}
 *   <li>{@code build.timestamp}
 *   <li>{@code git.commit.id.abbrev}
 * </ul>
 */
@Configuration
@ConditionalOnClass(InfoContributor.class)
@ConditionalOnProperty(
    prefix = "enterprise.common.info",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ApplicationInfoContributor {

  /** Actuator info contributor bean. */
  @Bean
  public InfoContributor enterpriseInfoContributor(
      @Value("${spring.application.name:unknown}") String appName,
      @Value("${spring.profiles.active:default}") String profile,
      @Value("${build.version:dev}") String buildVersion,
      @Value("${build.timestamp:}") String buildTimestamp,
      @Value("${git.commit.id.abbrev:}") String gitCommit) {

    return (Info.Builder builder) -> {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("name", appName);
      map.put("profile", profile);
      map.put("startedAt", Instant.now().toString());
      map.put("java", System.getProperty("java.version"));
      if (buildVersion != null && !buildVersion.isBlank()) map.put("version", buildVersion);
      if (buildTimestamp != null && !buildTimestamp.isBlank())
        map.put("buildTimestamp", buildTimestamp);
      if (gitCommit != null && !gitCommit.isBlank()) map.put("commit", gitCommit);
      builder.withDetails(map);
    };
  }
}
