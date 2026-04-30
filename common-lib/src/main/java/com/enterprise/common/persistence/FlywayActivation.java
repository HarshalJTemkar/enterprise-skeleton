package com.enterprise.common.persistence;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Activation marker for Flyway. Spring Boot auto-configures the
 * {@link Flyway} bean from the standard {@code spring.flyway.*} properties;
 * this class only:
 *
 * <ul>
 *   <li>Confirms Flyway is on the classpath before the rest of the platform
 *       relies on it.</li>
 *   <li>Honors a single platform-level toggle so dev profiles can disable
 *       migrations without touching {@code spring.flyway.enabled}.</li>
 * </ul>
 *
 * <p>Recommended migration locations (default in Spring Boot):</p>
 * <pre>
 *   src/main/resources/db/migration/V1__init.sql
 *   src/main/resources/db/migration/V2__add_users.sql
 * </pre>
 */
@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(prefix = "enterprise.common.flyway", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class FlywayActivation {
}
