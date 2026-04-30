package com.enterprise.common.enums;

/**
 * Lifecycle status common to most aggregates in the platform.
 * Prefer this over ad-hoc boolean {@code active}/{@code deleted} fields so
 * filters and reports remain uniform across services.
 */
public enum AppStatus {
    /** Entity is live and participates in business flows. */
    ACTIVE,
    /** Temporarily disabled; retained for audit but hidden from normal queries. */
    INACTIVE,
    /** Soft-deleted; never surfaced to clients, may be purged by retention jobs. */
    DELETED,
    /** Created but awaiting activation (e.g. email verification). */
    PENDING,
    /** Permanently archived; read-only. */
    ARCHIVED
}
