package com.enterprise.auth.token;

import com.enterprise.common.config.CommonProperties;
import com.enterprise.common.exception.BusinessException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.common.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists, validates and rotates refresh tokens.
 *
 * <p>Refresh tokens are still signed JWTs (so they remain stateless w.r.t.
 * the user identity) but every issued token is also fingerprinted via
 * SHA-256 and stored. {@code /refresh} only succeeds if the fingerprint
 * exists, is not revoked and not expired – making forged or replayed
 * tokens detectable, and giving us a real {@code /logout} primitive.</p>
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final JwtTokenProvider tokens;
    private final CommonProperties props;

    public RefreshTokenService(RefreshTokenRepository repo,
                               JwtTokenProvider tokens,
                               CommonProperties props) {
        this.repo = repo;
        this.tokens = tokens;
        this.props = props;
    }

    public static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Transactional
    public String issue(String username, HttpServletRequest req) {
        String raw = tokens.generateRefreshToken(username);
        RefreshTokenEntity e = new RefreshTokenEntity();
        e.setTokenHash(hash(raw));
        e.setUsername(username);
        e.setIssuedAt(Instant.now());
        e.setExpiresAt(Instant.now().plus(props.jwt().refreshTokenTtl()));
        e.setRevoked(false);
        if (req != null) {
            e.setUserAgent(truncate(req.getHeader("User-Agent"), 255));
            e.setIpAddress(truncate(req.getRemoteAddr(), 64));
        }
        repo.save(e);
        return raw;
    }

    /**
     * Validates and rotates a refresh token. The supplied token is revoked
     * and a freshly issued one is returned together with the identity it
     * authenticates.
     */
    @Transactional
    public Rotation rotate(String rawRefresh, HttpServletRequest req) {
        Claims claims;
        try {
            claims = tokens.parse(rawRefresh);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Invalid refresh token");
        }
        if (!"refresh".equals(claims.get("type"))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Not a refresh token");
        }
        String hash = hash(rawRefresh);
        RefreshTokenEntity stored = repo.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "Refresh token not recognised"));

        if (!stored.isActive(Instant.now())) {
            // Re-use of a revoked / expired token → revoke everything for this user as a safety net.
            repo.revokeAllForUser(stored.getUsername());
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Refresh token revoked");
        }

        String username = claims.getSubject();
        if (!username.equals(stored.getUsername())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Refresh token subject mismatch");
        }

        // Rotate
        stored.setRevoked(true);
        String newRaw = issue(username, req);
        stored.setReplacedBy(hash(newRaw));
        return new Rotation(username, newRaw);
    }

    @Transactional
    public void revoke(String rawRefresh) {
        Optional<RefreshTokenEntity> stored = repo.findByTokenHash(hash(rawRefresh));
        stored.ifPresent(e -> e.setRevoked(true));
    }

    @Transactional
    public int revokeAll(String username) {
        return repo.revokeAllForUser(username);
    }

    /** Daily cleanup of expired rows. */
    @Scheduled(cron = "${auth.refresh-tokens.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpired() {
        repo.deleteExpired(Instant.now());
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    public record Rotation(String username, String rawRefreshToken) {
        public List<String> rolesClaim() { return List.of(); }
    }
}
