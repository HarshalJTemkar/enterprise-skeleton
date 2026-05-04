package com.enterprise.common.test;

import java.util.Arrays;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * Spring Security {@link WithSecurityContextFactory} backing {@link WithMockJwtUser}. Builds a
 * {@link UsernamePasswordAuthenticationToken} with the requested roles, mirroring what the
 * production {@code JwtAuthenticationFilter} would have produced.
 */
public class WithMockJwtUserSecurityContextFactory
    implements WithSecurityContextFactory<WithMockJwtUser> {

  @Override
  public SecurityContext createSecurityContext(WithMockJwtUser ann) {
    String subject = ann.subject().isBlank() ? ann.username() : ann.subject();
    List<GrantedAuthority> authorities =
        Arrays.stream(ann.roles())
            .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
            .map(SimpleGrantedAuthority::new)
            .map(GrantedAuthority.class::cast)
            .toList();
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(subject, "n/a", authorities);
    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
    ctx.setAuthentication(auth);
    return ctx;
  }
}
