package com.enterprise.common.domain;

import com.enterprise.common.enums.AppStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;

/**
 * Marker subclass of {@link BaseEntity} for aggregates whose lifecycle you want to audit via {@link
 * AppStatus} in addition to the standard timestamps. Add further audit columns here if needed (e.g.
 * approvedBy, archivedAt).
 */
@MappedSuperclass
public abstract class AuditableEntity extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 16, nullable = false)
  private AppStatus status = AppStatus.ACTIVE;

  public AppStatus getStatus() {
    return status;
  }

  public void setStatus(AppStatus status) {
    this.status = status;
  }
}
