package com.enterprise.common.security;

import com.enterprise.common.config.CommonProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;

/**
 * Small, self-contained JWT helper. Registered as a {@code @Bean} by the
 * platform's auto-configuration when JJWT is on the classpath and
 * {@code enterprise.common.security.enabled=true}.
 */
public class JwtTokenProvider {

    private final CommonProperties props;
    private final SecretKey key;

    public JwtTokenProvider(CommonProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String subject, List<String> roles, Map<String, Object> extra) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuer(props.jwt().issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.jwt().accessTokenTtl())))
                .claim("roles", roles)
                .claims(extra == null ? Map.of() : extra)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuer(props.jwt().issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.jwt().refreshTokenTtl())))
                .claim("type", "refresh")
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
