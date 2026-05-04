package com.enterprise.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base for every JPA entity in the platform. Provides:
 *
 * <ul>
 *   <li>auto-generated surrogate {@link Long} id
 *   <li>optimistic-locking {@code version}
 *   <li>Spring Data JPA auditing columns ({@code createdAt / updatedAt / createdBy / updatedBy})
 *       populated automatically when auditing is enabled with {@code @EnableJpaAuditing}
 * </ul>
 *
 * <p>Equality is based on identifier + class so Hibernate proxies compare correctly.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private Instant updatedAt;

  @CreatedBy
  @Column(name = "created_by", length = 64, updatable = false)
  private String createdBy;

  @LastModifiedBy
  @Column(name = "updated_by", length = 64)
  private String updatedBy;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BaseEntity that = (BaseEntity) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getClass());
  }
}
