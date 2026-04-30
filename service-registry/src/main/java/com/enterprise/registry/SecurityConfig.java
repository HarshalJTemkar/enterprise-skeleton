package com.enterprise.registry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(c -> c.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/**", "/eureka/**").permitAll()
                .anyRequest().authenticated())
            .httpBasic(b -> {});
        return http.build();
    }
}
