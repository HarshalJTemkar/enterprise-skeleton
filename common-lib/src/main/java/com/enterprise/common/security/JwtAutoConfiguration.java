package com.enterprise.common.security;

import com.enterprise.common.config.CommonProperties;
import io.jsonwebtoken.Jwts;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the platform's {@link JwtTokenProvider} bean.
 *
 * <p>Active only when:
 *
 * <ul>
 *   <li>JJWT ({@link Jwts}) is on the classpath
 *   <li>{@code enterprise.common.security.enabled=true}
 * </ul>
 *
 * <p>Both the auth-service (issues tokens) and any resource server (validates tokens) consume the
 * same bean — keeping signing keys / TTLs in one place.
 */
@Configuration
@ConditionalOnClass(Jwts.class)
@ConditionalOnProperty(
    prefix = "enterprise.common.security",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
@EnableConfigurationProperties(CommonProperties.class)
public class JwtAutoConfiguration {

  /** The shared {@link JwtTokenProvider}. */
  @Bean
  @ConditionalOnMissingBean
  public JwtTokenProvider jwtTokenProvider(CommonProperties props) {
    return new JwtTokenProvider(props);
  }
}
