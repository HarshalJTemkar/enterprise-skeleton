package com.enterprise.common.security;

import com.enterprise.common.config.CommonProperties;
import com.enterprise.common.enums.HttpHeaderConstants;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-service JWT validation filter. Runs <em>once</em> per request and:
 * <ol>
 *   <li>Reads the configured {@code Authorization} header (default
 *       {@code Authorization: Bearer …}).</li>
 *   <li>Verifies the signature with {@link JwtTokenProvider} using the same
 *       shared secret the gateway uses.</li>
 *   <li>Builds an {@link Authentication} carrying the subject and the roles
 *       claim and stores it in the {@link SecurityContextHolder}.</li>
 *   <li>Pushes {@code userId} into the SLF4J MDC for log correlation.</li>
 *   <li>If the token is missing/invalid the filter does nothing — Spring
 *       Security's {@code AuthenticationEntryPoint} will reject protected
 *       endpoints later.</li>
 * </ol>
 *
 * <p>Activate via {@code enterprise.common.security.resource-server.enabled=true}.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;
    private final CommonProperties props;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CommonProperties props) {
        this.tokenProvider = tokenProvider;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String header = request.getHeader(props.jwt().headerName());
            String prefix = props.jwt().tokenPrefix();
            if (header != null && header.startsWith(prefix)) {
                String token = header.substring(prefix.length()).trim();
                if (tokenProvider.isValid(token)) {
                    Claims claims = tokenProvider.parse(token);
                    Authentication auth = toAuthentication(claims);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    MDC.put("userId", String.valueOf(claims.getSubject()));
                }
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
        }
    }

    /** Translate JWT claims into a Spring Security {@link Authentication}. */
    @SuppressWarnings("unchecked")
    private Authentication toAuthentication(Claims claims) {
        Object roles = claims.get("roles");
        Collection<GrantedAuthority> authorities = List.of();
        if (roles instanceof Collection<?> c) {
            authorities = c.stream()
                    .map(String::valueOf)
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .map(GrantedAuthority.class::cast)
                    .toList();
        }
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
        auth.setDetails(claims);
        return auth;
    }

    /**
     * Skip the filter for typical public endpoints. Override in a subclass
     * (or via a {@link org.springframework.security.web.SecurityFilterChain})
     * for finer-grained control.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator/")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-ui")
                || uri.equals("/swagger-ui.html")
                // Convenience: read-through gateway-provided header
                || request.getHeader(HttpHeaderConstants.AUTH_SUBJECT) != null;
    }
}
