# 0003. JWT-based stateless auth with persisted refresh tokens

* Status: accepted
* Date: 2026-04-27
* Deciders: platform-team

## Context

The skeleton needs an authentication scheme that:

1. is stateless on the API edge (no sticky sessions);
2. supports **logout** and **token revocation** (regulatory + UX);
3. fits a low-ops footprint (no full IdP at first).

## Decision

* Issue **HS256-signed access JWTs** (1 h TTL) and **refresh JWTs** (7 d TTL).
* Persist a **SHA-256 fingerprint** of every refresh token in
  `refresh_tokens`. The raw token never leaves the client.
* On `/refresh`, the supplied token is **revoked** and a new one is issued
  (rotation). Re-use of a revoked token revokes *all* tokens for that user
  (replay detection).
* `/logout` revokes a single token; `/logout/all` revokes every active token
  for the principal.
* Per-user **lockout** after N failed login attempts (default 5) for
  `auth.lockout.duration` (default 15 min); plus a global Resilience4j
  `RateLimiter` on `/login`.

## Consequences

* The auth-service is **not** fully stateless (refresh tokens hit the DB),
  but access-token validation by the gateway remains stateless and cheap.
* Switching to OIDC (Keycloak, Auth0) later is straightforward — replace
  `JwtTokenProvider` and the issuer/audience properties; the gateway filter
  pattern stays.
* DB grows linearly with logins; mitigated by a daily `@Scheduled` purge of
  expired rows.

## Alternatives considered

* **Pure stateless JWT (no DB)** — rejected: cannot revoke / logout.
* **Server-side opaque tokens** — heavier on the gateway (per-request DB
  lookup), but simpler revocation. Reconsider if traffic profile allows.
* **OIDC from day 1** — too much ops for a starter; left as drop-in path.
