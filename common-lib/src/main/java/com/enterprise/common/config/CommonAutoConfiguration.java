package com.enterprise.common.config;

import com.enterprise.common.exception.GlobalExceptionHandler;
import com.enterprise.common.i18n.MessageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Auto-configuration for common platform features. All features are opt-in / toggleable.
 */
@Configuration
@EnableConfigurationProperties(CommonProperties.class)
public class CommonAutoConfiguration {

    /**
     * Registers the RFC-7807 global exception handler when the app is a
     * servlet-based web module and {@link MessageService} is available.
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
    @ConditionalOnBean(MessageService.class)
    @ConditionalOnProperty(
            prefix = "enterprise.common.web.exception-handler",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public GlobalExceptionHandler globalExceptionHandler(MessageService messageService) {
        return new GlobalExceptionHandler(messageService);
    }
}
