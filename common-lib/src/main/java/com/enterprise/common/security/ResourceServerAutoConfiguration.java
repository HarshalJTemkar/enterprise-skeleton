package com.enterprise.common.security;

import com.enterprise.common.config.CommonProperties;
import com.enterprise.common.i18n.MessageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Wires the platform's resource-server primitives:
 * <ul>
 *   <li>{@link JwtAuthenticationFilter} – validates the bearer token and
 *       populates the security context.</li>
 *   <li>{@link ProblemDetailAuthenticationEntryPoint} – emits RFC 7807
 *       responses for 401 / 403.</li>
 * </ul>
 *
 * <p>The shared filter chain still lives in each service so they can decide
 * which routes to protect. Toggle with
 * {@code enterprise.common.security.resource-server.enabled=true}.</p>
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.security.web.SecurityFilterChain")
@ConditionalOnProperty(prefix = "enterprise.common.security.resource-server",
        name = "enabled", havingValue = "true", matchIfMissing = false)
public class ResourceServerAutoConfiguration {

    /** JWT validation filter (registered as a regular bean; service config wires it into the chain). */
    @Bean
    @ConditionalOnBean(JwtTokenProvider.class)
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider provider,
                                                           CommonProperties props) {
        return new JwtAuthenticationFilter(provider, props);
    }

    /** Single bean implements both Spring Security extension points. */
    @Bean
    @ConditionalOnMissingBean({AuthenticationEntryPoint.class, AccessDeniedHandler.class})
    public ProblemDetailAuthenticationEntryPoint problemDetailAuthenticationEntryPoint(
            MessageService messageService) {
        return new ProblemDetailAuthenticationEntryPoint(messageService);
    }
}
