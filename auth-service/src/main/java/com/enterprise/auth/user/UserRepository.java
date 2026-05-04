package com.enterprise.auth.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link UserEntity}. Uses an entity graph to fetch roles eagerly in a single query.
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

  /** Look up by login name; eagerly loads roles to avoid N+1. */
  @EntityGraph(attributePaths = "roles")
  Optional<UserEntity> findByUsername(String username);

  /** Existence check used by registration flows. */
  boolean existsByUsername(String username);

  /** Existence check used by registration flows. */
  boolean existsByEmail(String email);
}
