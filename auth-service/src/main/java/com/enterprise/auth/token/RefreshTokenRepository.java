package com.enterprise.auth.token;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revoked = true where t.username = :username and t.revoked = false")
    int revokeAllForUser(@Param("username") String username);

    @Modifying
    @Query("delete from RefreshTokenEntity t where t.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
