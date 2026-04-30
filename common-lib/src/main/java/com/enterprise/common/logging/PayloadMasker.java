package com.enterprise.common.logging;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Masks sensitive fields inside JSON / form-encoded payloads before they are
 * written to logs. Uses simple regular expressions — fast and dependency-free.
 *
 * <p>Handles these payload shapes:</p>
 * <ul>
 *   <li>JSON: {@code "password":"abc"} → {@code "password":"***"}</li>
 *   <li>Form / query: {@code password=abc} → {@code password=***}</li>
 *   <li>Authorization header: {@code Bearer xxx} → {@code Bearer ***}</li>
 * </ul>
 */
public class PayloadMasker {

    private final boolean enabled;
    private final String mask;
    private final List<Pattern> jsonPatterns;
    private final List<Pattern> formPatterns;
    private static final Pattern BEARER = Pattern.compile(
            "(?i)(Bearer\\s+)[A-Za-z0-9\\-_.=]+");

    public PayloadMasker(LoggingProperties.Masking cfg) {
        this.enabled = cfg.isEnabled();
        this.mask = cfg.getMask();
        this.jsonPatterns = cfg.getFields().stream()
                .map(f -> Pattern.compile(
                        "(\"" + Pattern.quote(f) + "\"\\s*:\\s*)(\".*?\"|\\d+|true|false|null)",
                        Pattern.CASE_INSENSITIVE))
                .toList();
        this.formPatterns = cfg.getFields().stream()
                .map(f -> Pattern.compile(
                        "([?&]?" + Pattern.quote(f) + "=)([^&\\s\"]+)",
                        Pattern.CASE_INSENSITIVE))
                .toList();
    }

    /**
     * Returns {@code payload} with every configured sensitive field replaced
     * by the mask token. Never throws on malformed input.
     *
     * @param payload the body / query string to sanitize (may be {@code null})
     * @return sanitized text, or the original when masking is disabled
     */
    public String mask(String payload) {
        if (!enabled || payload == null || payload.isEmpty()) return payload;
        String out = payload;
        for (Pattern p : jsonPatterns) {
            out = p.matcher(out).replaceAll("$1\"" + mask + "\"");
        }
        for (Pattern p : formPatterns) {
            out = p.matcher(out).replaceAll("$1" + mask);
        }
        out = BEARER.matcher(out).replaceAll("$1" + mask);
        return out;
    }
}
