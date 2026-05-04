package com.enterprise.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a single shared {@link OpenAPI} bean consumed by SpringDoc.
 *
 * <p>Active only when:
 *
 * <ul>
 *   <li>SpringDoc {@link OpenAPI} class is on the classpath
 *   <li>{@code enterprise.common.openapi.enabled=true} (default)
 *   <li>The application has not already defined its own {@link OpenAPI} bean
 * </ul>
 */
@Configuration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnProperty(
    prefix = "enterprise.common.openapi",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(OpenApiProperties.class)
public class OpenApiAutoConfiguration {

  /**
   * Builds the shared OpenAPI descriptor from {@link OpenApiProperties}. If {@link
   * OpenApiProperties#isJwtSecurityEnabled()} is true, a {@code bearerAuth} security scheme of type
   * HTTP/JWT is also registered and applied globally.
   *
   * @param props externalized configuration bean
   * @return the singleton OpenAPI document
   */
  @Bean
  @ConditionalOnMissingBean
  public OpenAPI enterpriseOpenAPI(OpenApiProperties props) {
    OpenAPI api = new OpenAPI().info(buildInfo(props));

    if (props.isJwtSecurityEnabled()) {
      String name = props.getSecuritySchemeName();
      api.addSecurityItem(new SecurityRequirement().addList(name))
          .components(
              new Components()
                  .addSecuritySchemes(
                      name,
                      new SecurityScheme()
                          .type(SecurityScheme.Type.HTTP)
                          .scheme("bearer")
                          .bearerFormat("JWT")
                          .description("JWT access token issued by auth-service")));
    }
    return api;
  }

  /**
   * Converts the property bean into an OpenAPI {@link Info} block (title, description, version,
   * contact, license).
   */
  private Info buildInfo(OpenApiProperties p) {
    return new Info()
        .title(p.getTitle())
        .description(p.getDescription())
        .version(p.getVersion())
        .contact(new Contact().name(p.getContactName()).email(p.getContactEmail()))
        .license(new License().name(p.getLicenseName()).url(p.getLicenseUrl()));
  }
}
