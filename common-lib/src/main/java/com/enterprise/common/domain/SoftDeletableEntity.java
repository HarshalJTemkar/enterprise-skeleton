package com.enterprise.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

/**
 * Entities extending this class are never physically removed: a
 * {@code deleted} flag and {@code deletedAt} timestamp are flipped instead.
 *
 * <p>Pair with a Hibernate {@code @SQLDelete("update … set deleted=true")}
 * and {@code @Where("deleted=false")} in concrete subclasses to make
 * standard repositories ignore soft-deleted rows automatically.</p>
 */
@MappedSuperclass
public abstract class SoftDeletableEntity extends AuditableEntity {

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 64)
    private String deletedBy;

    /** Flip the flags. Call from the service layer; do <em>not</em> call {@code delete()}. */
    public void softDelete(String actor) {
        this.deleted = true;
        this.deletedAt = Instant.now();
        this.deletedBy = actor;
    }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }
}
