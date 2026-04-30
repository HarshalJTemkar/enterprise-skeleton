package com.enterprise.auth.token;

import com.enterprise.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Persistent refresh-token record. Only the SHA-256 hash of the raw token is
 * stored, so a database leak never exposes a usable refresh token.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Created on successful {@code /login}.</li>
 *   <li>On {@code /refresh}: marked {@code revoked=true} and a new token is
 *   issued (rotation). {@code replacedBy} points at the new token's hash.</li>
 *   <li>{@code /logout} revokes either the supplied token or all tokens for
 *   the principal.</li>
 * </ol>
 */
@Entity
@Table(name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_tokens_username", columnList = "username"),
                @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
        })
public class RefreshTokenEntity extends BaseEntity {

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "replaced_by", length = 128)
    private String replacedBy;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    public boolean isActive(Instant now) {
        return !revoked && expiresAt.isAfter(now);
    }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String v) { this.tokenHash = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant v) { this.issuedAt = v; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant v) { this.expiresAt = v; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean v) { this.revoked = v; }
    public String getReplacedBy() { return replacedBy; }
    public void setReplacedBy(String v) { this.replacedBy = v; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String v) { this.userAgent = v; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String v) { this.ipAddress = v; }
}
