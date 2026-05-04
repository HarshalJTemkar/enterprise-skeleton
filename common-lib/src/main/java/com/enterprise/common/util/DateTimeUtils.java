package com.enterprise.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** Date / time helpers centered on ISO-8601 and UTC. */
public final class DateTimeUtils {

  /** ISO-8601 formatter bound to UTC, e.g. {@code 2026-01-01T00:00:00Z}. */
  public static final DateTimeFormatter ISO_UTC =
      DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

  private DateTimeUtils() {}

  /** Current instant rendered as ISO-8601 UTC. */
  public static String nowIsoUtc() {
    return ISO_UTC.format(Instant.now());
  }

  /** Render any {@link Instant} as ISO-8601 UTC (null → {@code null}). */
  public static String toIsoUtc(Instant instant) {
    return instant == null ? null : ISO_UTC.format(instant);
  }

  /** Convert an {@link Instant} to a local date/time in the given zone. */
  public static LocalDateTime toZoned(Instant instant, ZoneId zone) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, zone);
  }

  /** Epoch millis → UTC {@link ZonedDateTime}. */
  public static ZonedDateTime fromEpochMillis(long epochMillis) {
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC);
  }

  /** Convenience: UTC {@link Instant} at the start of {@code date}. */
  public static Instant startOfDayUtc(LocalDate date) {
    return date.atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  /** Convenience: UTC {@link Instant} at the end of {@code date} (23:59:59.999). */
  public static Instant endOfDayUtc(LocalDate date) {
    return date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1);
  }
}
