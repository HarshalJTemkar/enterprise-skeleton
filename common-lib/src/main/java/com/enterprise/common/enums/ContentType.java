package com.enterprise.common.enums;

/** Canonical media types used across the platform. */
public final class ContentType {
    public static final String JSON = "application/json";
    public static final String JSON_UTF8 = "application/json;charset=UTF-8";
    public static final String PROBLEM_JSON = "application/problem+json";
    public static final String XML = "application/xml";
    public static final String FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String MULTIPART = "multipart/form-data";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String PDF = "application/pdf";
    public static final String OCTET_STREAM = "application/octet-stream";

    private ContentType() {}
}
