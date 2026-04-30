package com.enterprise.common.enums;

/** Sort direction accepted by REST list endpoints. */
public enum SortDirection {
    ASC, DESC;

    /** Parse case-insensitively, defaulting to {@link #ASC}. */
    public static SortDirection from(String s) {
        if (s == null || s.isBlank()) return ASC;
        try { return SortDirection.valueOf(s.toUpperCase()); } catch (Exception e) { return ASC; }
    }
}
