package com.enterprise.common.util;

import com.enterprise.common.enums.SortDirection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Helpers for turning raw query parameters into a Spring Data {@link Pageable}, with safe caps on
 * page size.
 */
public final class PaginationUtils {

  /** Hard cap so clients cannot request huge pages. */
  public static final int MAX_PAGE_SIZE = 200;

  private PaginationUtils() {}

  /**
   * Build a {@link Pageable} from typical query parameters.
   *
   * @param page 0-based page number (null → 0)
   * @param size page size (null → 20, capped at {@link #MAX_PAGE_SIZE})
   * @param sortBy field name (may be null)
   * @param direction {@link SortDirection} (may be null → ASC)
   */
  public static Pageable of(Integer page, Integer size, String sortBy, SortDirection direction) {
    int p = page == null || page < 0 ? 0 : page;
    int s = size == null || size <= 0 ? 20 : Math.min(size, MAX_PAGE_SIZE);
    if (sortBy == null || sortBy.isBlank()) {
      return PageRequest.of(p, s);
    }
    Sort.Direction d = direction == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
    return PageRequest.of(p, s, Sort.by(d, sortBy));
  }

  /** Overload that parses the string form of {@link SortDirection}. */
  public static Pageable of(Integer page, Integer size, String sortBy, String direction) {
    return of(page, size, sortBy, SortDirection.from(direction));
  }
}
