package com.enterprise.common.i18n;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code enterprise.common.i18n.*}. Drives {@code MessageSource} + {@code LocaleResolver}
 * registration.
 */
@ConfigurationProperties(prefix = "enterprise.common.i18n")
public class I18nProperties {

  /** Master switch for i18n auto-configuration. */
  private boolean enabled = true;

  /** Base names of resource bundles to load, without the {@code .properties} suffix. */
  private List<String> basenames = List.of("classpath:i18n/messages", "classpath:i18n/errors");

  /** Default locale used when no {@code Accept-Language} header is present. */
  private String defaultLocale = "en";

  /** How long to cache the parsed bundles before reloading. */
  private Duration cacheDuration = Duration.ofMinutes(10);

  /** Charset of the bundles (UTF-8 required for non-ASCII messages). */
  private String encoding = "UTF-8";

  /** If {@code true}, returns the key when a translation is missing instead of throwing. */
  private boolean useCodeAsDefaultMessage = true;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<String> getBasenames() {
    return basenames;
  }

  public void setBasenames(List<String> v) {
    this.basenames = v;
  }

  public String getDefaultLocale() {
    return defaultLocale;
  }

  public void setDefaultLocale(String v) {
    this.defaultLocale = v;
  }

  public Duration getCacheDuration() {
    return cacheDuration;
  }

  public void setCacheDuration(Duration v) {
    this.cacheDuration = v;
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String v) {
    this.encoding = v;
  }

  public boolean isUseCodeAsDefaultMessage() {
    return useCodeAsDefaultMessage;
  }

  public void setUseCodeAsDefaultMessage(boolean v) {
    this.useCodeAsDefaultMessage = v;
  }
}
