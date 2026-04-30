package com.enterprise.auth.user;

import com.enterprise.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Persistent role. Names are stored without the {@code ROLE_} prefix; the
 * {@link com.enterprise.auth.user.JpaUserDetailsService} adds it when
 * adapting to Spring Security {@code GrantedAuthority}s.
 */
@Entity
@Table(name = "roles")
public class RoleEntity extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 32)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
}
