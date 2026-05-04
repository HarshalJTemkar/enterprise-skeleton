package com.enterprise.common.openapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties controlling the auto-generated OpenAPI / Swagger-UI document.
 *
 * <p>All fields are overridable under the {@code enterprise.common.openapi.*} prefix in {@code
 * application.yml}. Setting {@link #enabled} to {@code false} completely disables the OpenAPI bean
 * registration.
 */
@ConfigurationProperties(prefix = "enterprise.common.openapi")
public class OpenApiProperties {

  /** Master switch for OpenAPI auto-configuration. Default {@code true}. */
  private boolean enabled = true;

  /** API title shown at the top of Swagger UI. */
  private String title = "Enterprise API";

  /** Long-form description of the API. */
  private String description = "REST API documentation";

  /** Semantic version string displayed in the Swagger header. */
  private String version = "1.0.0";

  /** Contact name displayed in the Swagger info block. */
  private String contactName = "Platform Team";

  /** Contact email displayed in the Swagger info block. */
  private String contactEmail = "platform@enterprise.com";

  /** License name (e.g. {@code Apache 2.0}). */
  private String licenseName = "Apache 2.0";

  /** Link to the license text. */
  private String licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0";

  /** When {@code true}, adds a global {@code bearerAuth} JWT security scheme. */
  private boolean jwtSecurityEnabled = true;

  /** Name of the security scheme registered in the OpenAPI components. */
  private String securitySchemeName = "bearerAuth";

  // ---- getters / setters ----
  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getContactName() {
    return contactName;
  }

  public void setContactName(String contactName) {
    this.contactName = contactName;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public String getLicenseName() {
    return licenseName;
  }

  public void setLicenseName(String licenseName) {
    this.licenseName = licenseName;
  }

  public String getLicenseUrl() {
    return licenseUrl;
  }

  public void setLicenseUrl(String licenseUrl) {
    this.licenseUrl = licenseUrl;
  }

  public boolean isJwtSecurityEnabled() {
    return jwtSecurityEnabled;
  }

  public void setJwtSecurityEnabled(boolean jwtSecurityEnabled) {
    this.jwtSecurityEnabled = jwtSecurityEnabled;
  }

  public String getSecuritySchemeName() {
    return securitySchemeName;
  }

  public void setSecuritySchemeName(String securitySchemeName) {
    this.securitySchemeName = securitySchemeName;
  }
}
