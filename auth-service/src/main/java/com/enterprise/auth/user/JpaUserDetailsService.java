package com.enterprise.auth.user;

import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed {@link UserDetailsService} consumed by Spring Security's {@code
 * DaoAuthenticationProvider}.
 *
 * <p>Replaces the in-memory user list previously kept in {@code application.yml}. Roles are mapped
 * to {@code ROLE_*} authorities so {@code hasRole("ADMIN")} works as expected.
 */
@Service
public class JpaUserDetailsService implements UserDetailsService {

  private final UserRepository users;

  public JpaUserDetailsService(UserRepository users) {
    this.users = users;
  }

  /** Look up a user by login. Inactive users are reported as locked. */
  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) {
    UserEntity u =
        users
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    return User.withUsername(u.getUsername())
        .password(u.getPasswordHash())
        .disabled(!u.isEnabled())
        .authorities(
            u.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
                .collect(Collectors.toSet()))
        .build();
  }
}
