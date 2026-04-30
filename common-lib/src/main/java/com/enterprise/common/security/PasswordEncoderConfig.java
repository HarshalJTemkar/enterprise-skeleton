package com.enterprise.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Registers a single shared {@link PasswordEncoder}.
 *
 * <p>When {@code enterprise.common.security.password-encoder.bcrypt-only=true}
 * a fixed BCrypt encoder is returned; otherwise Spring's delegating encoder
 * is used (supports {@code {bcrypt}, {noop}, {argon2}} and friends).</p>
 */
@Configuration
@ConditionalOnClass(PasswordEncoder.class)
public class PasswordEncoderConfig {

    /** The shared password encoder bean. */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "enterprise.common.security.password-encoder",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    public PasswordEncoder passwordEncoder(
            @org.springframework.beans.factory.annotation.Value(
                    "${enterprise.common.security.password-encoder.bcrypt-only:false}") boolean bcryptOnly,
            @org.springframework.beans.factory.annotation.Value(
                    "${enterprise.common.security.password-encoder.bcrypt-strength:10}") int strength) {
        return bcryptOnly
                ? new BCryptPasswordEncoder(strength)
                : PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
