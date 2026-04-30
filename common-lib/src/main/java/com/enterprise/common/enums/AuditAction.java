package com.enterprise.common.enums;

/** Verbs understood by the audit framework. Extend as needed. */
public enum AuditAction {
    CREATE, UPDATE, DELETE, READ,
    LOGIN, LOGOUT,
    ARCHIVE, RESTORE,
    EXPORT, IMPORT,
    APPROVE, REJECT
}
