package com.enterprise.common.exception;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Registers the {@link ResilienceExceptionAdvice} only when Resilience4j is on the classpath.
 * Keeping it conditional means {@code common-lib} can be consumed by services that don't use
 * Resilience4j without classloader issues at startup.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "io.github.resilience4j.circuitbreaker.CallNotPermittedException")
public class ResilienceExceptionAdviceAutoConfiguration {

  @Bean
  public ResilienceExceptionAdvice resilienceExceptionAdvice() {
    return new ResilienceExceptionAdvice();
  }
}
