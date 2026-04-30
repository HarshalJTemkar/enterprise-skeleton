package com.enterprise.auth.user;

import com.enterprise.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Persistent user aggregate. Inherits the standard auditing columns
 * ({@code id}, {@code version}, {@code createdAt/By}, {@code updatedAt/By},
 * {@code status}) from {@link AuditableEntity}.
 *
 * <p>The {@code passwordHash} column stores a Spring-Security-prefixed value
 * (e.g. {@code {bcrypt}…}) so the delegating {@code PasswordEncoder} can
 * upgrade strategies later without a schema change.</p>
 */
@Entity
@Table(name = "users")
public class UserEntity extends AuditableEntity {

    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles = new HashSet<>();

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public Set<RoleEntity> getRoles() { return roles; }
    public void setRoles(Set<RoleEntity> v) { this.roles = v; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int v) { this.failedLoginAttempts = v; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant v) { this.lockedUntil = v; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant v) { this.lastLoginAt = v; }
}
