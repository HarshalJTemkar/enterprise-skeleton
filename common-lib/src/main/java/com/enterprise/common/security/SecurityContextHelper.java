package com.enterprise.common.security;

import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Ergonomic, null-safe helpers for reading the current {@link Authentication}
 * out of the {@link SecurityContextHolder}.
 *
 * <p>Uses only static methods so it can be called from anywhere (services,
 * JPA auditors, Kafka consumers) without injecting a Spring bean.</p>
 */
public final class SecurityContextHelper {

    private SecurityContextHelper() {}

    /** @return the current {@link Authentication} if one is present. */
    public static Optional<Authentication> authentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(ctx -> ctx.getAuthentication());
    }

    /** @return the currently authenticated principal's name, if any. */
    public static Optional<String> currentUsername() {
        return authentication()
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName);
    }

    /**
     * @return the principal cast to {@link UserDetails} if applicable,
     * otherwise {@link Optional#empty()}.
     */
    public static Optional<UserDetails> currentUserDetails() {
        return authentication()
                .map(Authentication::getPrincipal)
                .filter(UserDetails.class::isInstance)
                .map(UserDetails.class::cast);
    }

    /** @return granted authority names, or an empty list when unauthenticated. */
    public static List<String> currentRoles() {
        return authentication()
                .map(a -> a.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .orElse(List.of());
    }

    /** @return {@code true} when the current user has the given role (checks both raw and {@code ROLE_} prefixed forms). */
    public static boolean hasRole(String role) {
        if (role == null) return false;
        String want = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return currentRoles().stream().anyMatch(r -> r.equals(role) || r.equals(want));
    }

    /** @return {@code true} when there is an authenticated non-anonymous user. */
    public static boolean isAuthenticated() {
        return authentication()
                .filter(Authentication::isAuthenticated)
                .map(a -> !"anonymousUser".equals(a.getPrincipal()))
                .orElse(false);
    }
}
