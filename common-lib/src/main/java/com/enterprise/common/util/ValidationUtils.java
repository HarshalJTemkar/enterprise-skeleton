package com.enterprise.common.util;

import java.util.regex.Pattern;

/** Centralized validator regexes and helpers. */
public final class ValidationUtils {

    /** RFC 5322-lite email pattern (practical, not exhaustive). */
    public static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$");

    /** E.164 phone number: optional '+', 7–15 digits. */
    public static final Pattern PHONE_E164 = Pattern.compile("^\\+?[1-9]\\d{6,14}$");

    /** Compact IBAN pattern (country + 2 check digits + up to 30 alphanumerics). */
    public static final Pattern IBAN = Pattern.compile("^[A-Z]{2}\\d{2}[A-Z0-9]{11,30}$");

    /** UUID pattern. */
    public static final Pattern UUID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private ValidationUtils() {}

    public static boolean isEmail(String s) { return s != null && EMAIL.matcher(s).matches(); }
    public static boolean isPhoneE164(String s) { return s != null && PHONE_E164.matcher(s).matches(); }
    public static boolean isIban(String s) { return s != null && IBAN.matcher(s.replace(" ", "").toUpperCase()).matches(); }
    public static boolean isUuid(String s) { return s != null && UUID.matcher(s).matches(); }
}
