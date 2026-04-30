package com.enterprise.common.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Null-safe string helpers: masking, sanitization, slugs. */
public final class StringUtils {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9-]+");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private StringUtils() {}

    /** {@code null}-safe blank check. */
    public static boolean isBlank(String s) { return s == null || s.isBlank(); }

    /** Returns {@code s} or {@code fallback} if blank. */
    public static String defaultIfBlank(String s, String fallback) {
        return isBlank(s) ? fallback : s;
    }

    /**
     * Mask all but the last {@code visible} characters of a string, useful for
     * displaying card numbers or emails: {@code mask("alice@x", 3)} → {@code ****@x}.
     */
    public static String mask(String value, int visible) {
        if (value == null) return null;
        int keep = Math.max(0, Math.min(visible, value.length()));
        int hide = value.length() - keep;
        return "*".repeat(hide) + value.substring(hide);
    }

    /** Collapse consecutive whitespace into single spaces and trim. */
    public static String normalizeWhitespace(String s) {
        if (s == null) return null;
        return WHITESPACE.matcher(s).replaceAll(" ").trim();
    }

    /**
     * Produce a URL-safe slug: lower-cased, diacritics stripped, spaces
     * become hyphens, non-alphanumerics removed.
     */
    public static String slugify(String s) {
        if (s == null) return null;
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        n = DIACRITICS.matcher(n).replaceAll("");
        n = n.toLowerCase().trim().replaceAll("\\s+", "-");
        return NON_SLUG.matcher(n).replaceAll("").replaceAll("-+", "-");
    }

    /** Remove characters commonly used in log-forging attempts. */
    public static String sanitizeForLog(String s) {
        if (s == null) return null;
        return s.replaceAll("[\\r\\n\\t]", "_");
    }
}
