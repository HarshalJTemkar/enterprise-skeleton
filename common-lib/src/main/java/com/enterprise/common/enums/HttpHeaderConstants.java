package com.enterprise.common.enums;

/** String constants for non-standard HTTP headers used by the platform. */
public final class HttpHeaderConstants {

  /** Correlation identifier propagated through the request chain. */
  public static final String CORRELATION_ID = "X-Correlation-ID";

  /** Request identifier unique per HTTP call (per-hop). */
  public static final String REQUEST_ID = "X-Request-ID";

  /** Idempotency key for unsafe mutating endpoints. */
  public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

  /** Subject pulled from a validated JWT at the gateway. */
  public static final String AUTH_SUBJECT = "X-Auth-Subject";

  /** Comma-separated roles pulled from a validated JWT at the gateway. */
  public static final String AUTH_ROLES = "X-Auth-Roles";

  /** Tenant discriminator (multi-tenancy). */
  public static final String TENANT_ID = "X-Tenant-ID";

  /** Trace identifier surfaced to clients for support tickets. */
  public static final String TRACE_ID = "X-Trace-ID";

  /** API version selected by the client. */
  public static final String API_VERSION = "X-API-Version";

  private HttpHeaderConstants() {}
}
