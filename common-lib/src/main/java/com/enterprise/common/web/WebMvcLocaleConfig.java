package com.enterprise.common.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Adds a {@link LocaleChangeInterceptor} so clients can override the locale with a query parameter
 * — handy for testing localized responses without setting {@code Accept-Language}:
 *
 * <pre>
 *   GET /api/users/123?lang=de
 * </pre>
 *
 * <p>The default parameter name is {@code lang}; configure with {@code
 * enterprise.common.i18n.param-name}.
 */
@Configuration
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnProperty(
    prefix = "enterprise.common.i18n",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class WebMvcLocaleConfig implements WebMvcConfigurer {

  private final String paramName;

  public WebMvcLocaleConfig(
      @org.springframework.beans.factory.annotation.Value(
              "${enterprise.common.i18n.param-name:lang}")
          String paramName) {
    this.paramName = paramName;
  }

  /**
   * Build the {@link LocaleChangeInterceptor} bean (also exposed so apps can reuse / override it).
   */
  @Bean
  public LocaleChangeInterceptor localeChangeInterceptor() {
    LocaleChangeInterceptor i = new LocaleChangeInterceptor();
    i.setParamName(paramName);
    i.setIgnoreInvalidLocale(true);
    return i;
  }

  /** Register the interceptor with Spring MVC. */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(localeChangeInterceptor());
  }
}
