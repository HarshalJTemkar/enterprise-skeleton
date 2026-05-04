package com.enterprise.common.i18n;

import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * Registers the shared {@link MessageSource} and {@link MessageService} beans based on {@link
 * I18nProperties}.
 *
 * <p>Locale is derived from the standard {@code Accept-Language} header; when absent, {@link
 * I18nProperties#getDefaultLocale()} is used. Add new bundles simply by dropping {@code
 * messages_xx.properties} into {@code i18n/} on the classpath — no code change required.
 */
@Configuration
@ConditionalOnProperty(
    prefix = "enterprise.common.i18n",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(I18nProperties.class)
public class I18nAutoConfiguration {

  /**
   * Reloadable {@link MessageSource}. Uses {@code ResourceBundleMessageSource} semantics but can
   * hot-reload during development (cache controlled by {@link I18nProperties#getCacheDuration()}).
   */
  @Bean
  @ConditionalOnMissingBean(MessageSource.class)
  public MessageSource messageSource(I18nProperties props) {
    ReloadableResourceBundleMessageSource src = new ReloadableResourceBundleMessageSource();
    src.setBasenames(props.getBasenames().toArray(String[]::new));
    src.setDefaultEncoding(props.getEncoding());
    src.setFallbackToSystemLocale(false);
    src.setUseCodeAsDefaultMessage(props.isUseCodeAsDefaultMessage());
    src.setCacheMillis(props.getCacheDuration().toMillis());
    src.setDefaultLocale(Locale.forLanguageTag(props.getDefaultLocale()));
    return src;
  }

  /** Facade consumed by application code to render localized strings. */
  @Bean
  @ConditionalOnMissingBean
  public MessageService messageService(MessageSource messageSource) {
    return new MessageService(messageSource);
  }

  /**
   * Servlet-specific configuration for LocaleResolver. Only loaded when servlet web application is
   * present.
   */
  @Configuration
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
  static class ServletLocaleConfiguration {

    /**
     * Picks the locale from the {@code Accept-Language} HTTP header, falling back to {@link
     * I18nProperties#getDefaultLocale()}.
     */
    @Bean
    @ConditionalOnMissingBean(name = "localeResolver")
    public org.springframework.web.servlet.LocaleResolver localeResolver(I18nProperties props) {
      org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver resolver =
          new org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver();
      resolver.setDefaultLocale(Locale.forLanguageTag(props.getDefaultLocale()));
      return resolver;
    }
  }
}
