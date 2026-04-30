package com.enterprise.auth.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link RoleEntity}. */
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    /** Find a role by case-sensitive name (e.g. {@code "ADMIN"}). */
    Optional<RoleEntity> findByName(String name);
}
