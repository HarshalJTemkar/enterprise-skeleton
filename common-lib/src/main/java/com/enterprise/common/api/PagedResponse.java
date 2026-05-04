package com.enterprise.common.api;

import com.enterprise.common.enums.SortDirection;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;

/**
 * Paginated envelope returned by list endpoints.
 *
 * <p>Shape mirrors {@link ApiResponse} with an additional {@code pagination} block instead of a
 * free-form {@code data} payload.
 *
 * @param <T> the row type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagedResponse<T>(
    String status, List<T> data, Pagination pagination, String correlationId, Instant timestamp) {

  /** Snapshot of the window returned by a list endpoint. */
  public record Pagination(
      int page,
      int size,
      long totalElements,
      int totalPages,
      boolean hasNext,
      boolean hasPrevious,
      String sortBy,
      SortDirection sortDirection) {}

  /**
   * Build a paged response from already-computed values (use this when Spring Data is not
   * involved).
   */
  public static <T> PagedResponse<T> of(
      List<T> items,
      int page,
      int size,
      long totalElements,
      String sortBy,
      SortDirection sortDirection) {
    int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    Pagination p =
        new Pagination(
            page,
            size,
            totalElements,
            totalPages,
            page + 1 < totalPages,
            page > 0,
            sortBy,
            sortDirection);
    return new PagedResponse<>(
        "success", items == null ? List.of() : items, p, MDC.get("correlationId"), Instant.now());
  }

  /**
   * Build a paged response from a Spring Data {@code Page}. Kept as an overload using {@code
   * Object} so {@code common-lib} does not force a hard dependency on spring-data-commons at
   * runtime.
   */
  public static <T> PagedResponse<T> fromSpringPage(Object springPage) {
    try {
      Class<?> pageIface = Class.forName("org.springframework.data.domain.Page");
      if (!pageIface.isInstance(springPage)) {
        throw new IllegalArgumentException("Not a Spring Data Page: " + springPage);
      }
      int page = (int) pageIface.getMethod("getNumber").invoke(springPage);
      int size = (int) pageIface.getMethod("getSize").invoke(springPage);
      long total = (long) pageIface.getMethod("getTotalElements").invoke(springPage);
      @SuppressWarnings("unchecked")
      List<T> content = (List<T>) pageIface.getMethod("getContent").invoke(springPage);
      return of(content, page, size, total, null, SortDirection.ASC);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Spring Data is not on the classpath", e);
    }
  }
}
