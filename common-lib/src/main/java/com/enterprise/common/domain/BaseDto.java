package com.enterprise.common.domain;

import java.time.Instant;

/**
 * Marker interface for DTOs. A record implementing this interface documents that it is an API
 * transport object — useful for reflection-based helpers, OpenAPI grouping and Javadoc.
 */
public interface BaseDto {
  /**
   * @return identifier (nullable on create).
   */
  default Long id() {
    return null;
  }

  /**
   * @return creation timestamp when known.
   */
  default Instant createdAt() {
    return null;
  }

  /**
   * @return last-modified timestamp when known.
   */
  default Instant updatedAt() {
    return null;
  }
}
