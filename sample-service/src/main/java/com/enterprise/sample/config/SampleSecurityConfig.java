package com.enterprise.sample.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless filter chain. Identity is established by the API gateway and propagated via {@code
 * X-Auth-Subject} / {@code X-Auth-Roles}; this service does NOT validate JWTs itself. In
 * production, enable {@code TrustedGatewayHeaderFilter} (common-lib) so direct calls bypassing the
 * gateway are refused with HTTP 403.
 */
@Configuration
public class SampleSecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(c -> c.disable())
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/graphql/**", "/graphiql/**", "/actuator/**")
                    .permitAll()
                    .anyRequest()
                    .permitAll())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    return http.build();
  }
}
