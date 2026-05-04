package com.enterprise.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Registers {@link TrustedGatewayHeaderFilter} as a servlet filter when {@code
 * enterprise.common.security.trusted-gateway.enabled=true}. The filter rejects requests that don't
 * carry the configured shared secret header — preventing direct access that bypasses the API
 * gateway and blocking spoofed {@code X-Auth-*} identity headers.
 *
 * <p>Configuration:
 *
 * <pre>
 * enterprise:
 *   common:
 *     security:
 *       trusted-gateway:
 *         enabled: true
 *         header-name: X-Gateway-Secret
 *         secret: ${GATEWAY_SHARED_SECRET}
 * </pre>
 */
@AutoConfiguration
@ConditionalOnClass(jakarta.servlet.Filter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
    prefix = "enterprise.common.security.trusted-gateway",
    name = "enabled",
    havingValue = "true")
public class TrustedGatewayAutoConfiguration {

  @Bean
  @ConfigurationProperties(prefix = "enterprise.common.security.trusted-gateway")
  public TrustedGatewayProperties trustedGatewayProperties() {
    return new TrustedGatewayProperties();
  }

  @Bean
  public FilterRegistrationBean<TrustedGatewayHeaderFilter> trustedGatewayHeaderFilter(
      TrustedGatewayProperties props) {
    var filter = new TrustedGatewayHeaderFilter(props.getHeaderName(), props.getSecret());
    var reg = new FilterRegistrationBean<>(filter);
    reg.setOrder(Ordered.HIGHEST_PRECEDENCE); // before correlation-id and auth filters
    reg.addUrlPatterns(props.getUrlPatterns().toArray(String[]::new));
    return reg;
  }

  /** Settings for the trusted-gateway filter. */
  public static class TrustedGatewayProperties {
    private boolean enabled = false;
    private String headerName = "X-Gateway-Secret";
    private String secret;
    private java.util.List<String> urlPatterns = java.util.List.of("/*");

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean v) {
      this.enabled = v;
    }

    public String getHeaderName() {
      return headerName;
    }

    public void setHeaderName(String v) {
      this.headerName = v;
    }

    public String getSecret() {
      return secret;
    }

    public void setSecret(String v) {
      this.secret = v;
    }

    public java.util.List<String> getUrlPatterns() {
      return urlPatterns;
    }

    public void setUrlPatterns(java.util.List<String> v) {
      this.urlPatterns = v;
    }
  }
}
