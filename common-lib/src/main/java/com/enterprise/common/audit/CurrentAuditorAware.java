package com.enterprise.common.audit;

import com.enterprise.common.security.SecurityContextHelper;
import java.util.Optional;
import org.springframework.data.domain.AuditorAware;

/**
 * Resolves the "current actor" used by Spring Data JPA auditing to populate {@code @CreatedBy
 * / @LastModifiedBy} fields on {@code BaseEntity}.
 *
 * <p>Order of resolution:
 *
 * <ol>
 *   <li>The authenticated principal name from {@link SecurityContextHelper}
 *   <li>{@code "system"} — used by background jobs and tests
 * </ol>
 */
public class CurrentAuditorAware implements AuditorAware<String> {

  @Override
  public Optional<String> getCurrentAuditor() {
    return Optional.of(SecurityContextHelper.currentUsername().orElse("system"));
  }
}
