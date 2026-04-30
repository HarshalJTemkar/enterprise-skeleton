package com.enterprise.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.enterprise.common.config.CommonProperties;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static CommonProperties props() {
        return new CommonProperties(null, null,
                new CommonProperties.Jwt(
                        "test-secret-test-secret-test-secret-test-secret",
                        Duration.ofMinutes(5),
                        Duration.ofDays(1),
                        "tests",
                        "Authorization",
                        "Bearer "));
    }

    @Test
    void access_token_round_trips_subject_roles_and_extras() {
        var p = new JwtTokenProvider(props());
        String t = p.generateAccessToken("alice", List.of("ROLE_ADMIN"), Map.of("tenant", "acme"));
        Claims c = p.parse(t);
        assertThat(c.getSubject()).isEqualTo("alice");
        assertThat(c.getIssuer()).isEqualTo("tests");
        assertThat(c.get("roles", List.class)).containsExactly("ROLE_ADMIN");
        assertThat(c.get("tenant")).isEqualTo("acme");
        assertThat(c.getExpiration().toInstant().isAfter(c.getIssuedAt().toInstant())).isTrue();
    }

    @Test
    void refresh_token_carries_type_claim() {
        var p = new JwtTokenProvider(props());
        Claims c = p.parse(p.generateRefreshToken("bob"));
        assertThat(c.getSubject()).isEqualTo("bob");
        assertThat(c.get("type")).isEqualTo("refresh");
    }

    @Test
    void isValid_returns_false_for_garbage() {
        var p = new JwtTokenProvider(props());
        assertThat(p.isValid("not.a.jwt")).isFalse();
        assertThat(p.isValid("")).isFalse();
    }

    @Test
    void token_signed_by_other_secret_does_not_validate() {
        var a = new JwtTokenProvider(props());
        var b = new JwtTokenProvider(new CommonProperties(null, null,
                new CommonProperties.Jwt(
                        "OTHER-secret-OTHER-secret-OTHER-secret-OTHER-secret",
                        Duration.ofMinutes(5), Duration.ofDays(1),
                        "tests", "Authorization", "Bearer ")));
        String t = a.generateAccessToken("x", List.of(), Map.of());
        assertThat(b.isValid(t)).isFalse();
    }
}
