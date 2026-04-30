package com.enterprise.common.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing globally and registers a default
 * {@link AuditorAware} that returns the current authenticated username (or
 * {@code "system"}).
 *
 * <p>Disabled when:</p>
 * <ul>
 *   <li>Spring Data JPA is not on the classpath</li>
 *   <li>{@code enterprise.common.jpa.auditing.enabled=false}</li>
 * </ul>
 *
 * <p>Apps that need a more elaborate auditor (e.g. tenant-aware) can simply
 * publish their own {@link AuditorAware} bean — the {@code @ConditionalOnMissingBean}
 * makes this one back off.</p>
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.data.jpa.repository.config.EnableJpaAuditing")
@ConditionalOnProperty(prefix = "enterprise.common.jpa.auditing", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableJpaAuditing(auditorAwareRef = "currentAuditorAware")
public class JpaAuditingConfig {

    /** The default {@link AuditorAware} backed by Spring Security context. */
    @Bean
    @ConditionalOnMissingBean
    public AuditorAware<String> currentAuditorAware() {
        return new CurrentAuditorAware();
    }
}
