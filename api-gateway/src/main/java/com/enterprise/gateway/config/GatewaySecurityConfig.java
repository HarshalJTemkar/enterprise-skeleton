package com.enterprise.gateway.config;

import com.enterprise.common.config.CommonProperties;
import com.enterprise.gateway.filter.JwtAuthenticationGatewayFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({GatewayJwtProperties.class, ThrottlingProperties.class})
public class GatewaySecurityConfig {

  @Bean
  @ConditionalOnProperty(
      prefix = "gateway.security.jwt",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public JwtAuthenticationGatewayFilter jwtAuthenticationGatewayFilter(
      CommonProperties commonProps, GatewayJwtProperties jwtProps) {
    return new JwtAuthenticationGatewayFilter(commonProps, jwtProps);
  }
}
