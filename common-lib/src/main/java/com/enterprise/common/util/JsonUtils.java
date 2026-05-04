package com.enterprise.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Optional;

/**
 * Thin wrapper around a shared {@link ObjectMapper} configured for the platform (Java Time enabled,
 * unknown properties tolerated, ISO instants).
 *
 * <p>Methods never throw – {@code null} / {@link Optional} is returned on failure and the caller
 * can decide whether to escalate.
 */
public final class JsonUtils {

  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private JsonUtils() {}

  /**
   * @return the shared {@link ObjectMapper} instance.
   */
  public static ObjectMapper mapper() {
    return MAPPER;
  }

  /** Safely serialize any object to a JSON string. */
  public static String toJson(Object value) {
    try {
      return value == null ? null : MAPPER.writeValueAsString(value);
    } catch (Exception e) {
      return null;
    }
  }

  /** Safely deserialize a JSON string into {@code type}. */
  public static <T> Optional<T> fromJson(String json, Class<T> type) {
    if (json == null || json.isBlank()) return Optional.empty();
    try {
      return Optional.ofNullable(MAPPER.readValue(json, type));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /** Safely deserialize into a generic type ({@code List<Foo>}, {@code Map<String,Bar>}, …). */
  public static <T> Optional<T> fromJson(String json, TypeReference<T> type) {
    if (json == null || json.isBlank()) return Optional.empty();
    try {
      return Optional.ofNullable(MAPPER.readValue(json, type));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
