# Security model

## Identity & tokens

| Token         | Lifetime  | Storage              | Rotation               |
|---------------|-----------|----------------------|------------------------|
| Access JWT    | 1 h       | client memory        | on every login/refresh |
| Refresh JWT   | 7 d       | DB (SHA-256 only)    | on every `/refresh`    |
| Gateway secret| quarterly | k8s Secret / Vault   | manual                 |

* All JWTs are HS256-signed by `enterprise.common.jwt.secret`.
* Refresh tokens are only persisted as `SHA-256(token)` — a DB leak does
  **not** allow reuse.
* Reuse of a revoked refresh token revokes **all** the user's tokens.

## Defense in depth

```
Internet
   │  TLS (Ingress)
   ▼
NGINX Ingress  ──── X-Forwarded-* headers added ────────────┐
   │                                                        │
   ▼                                                        │
api-gateway  ── validates JWT, drops bad headers ──────────►│
   │  X-Auth-Subject, X-Auth-Roles, X-Correlation-ID        │
   ▼                                                        │
TrustedGatewayHeaderFilter (downstream)                     │
   │  rejects 403 if X-Gateway-Secret missing/wrong         │
   ▼                                                        │
Application code  ── never trusts headers without filter ───┘
```

## Threat → control matrix

| Threat                         | Control                                                         |
|--------------------------------|-----------------------------------------------------------------|
| Brute force on login           | RateLimiter `auth-login`, per-user lockout                      |
| Credential stuffing            | Optional Have-I-Been-Pwned hook (extension point)               |
| Token theft (XSS)              | Short access TTL, refresh rotation, `/logout/all`               |
| Replay of refresh token        | Persisted hash + reuse-detection revokes all                    |
| Spoofed identity headers       | `TrustedGatewayHeaderFilter` (constant-time secret compare)     |
| Direct service access          | NetworkPolicies + above filter                                  |
| Sensitive data in logs         | `PayloadMasker` (JSON / form / Bearer) + audit log allow-list   |
| CSRF on browser flows          | Stateless APIs + `SameSite=strict` for any browser cookies      |
| Container escape               | Non-root, read-only rootfs, `seccompProfile: RuntimeDefault`    |
| Supply-chain (CVEs)            | CycloneDX SBOM + OWASP DC + Trivy fs/image in CI                |
| Config drift                   | GitOps + Spring Cloud Config Server `/actuator/refresh`         |
| Dependency typosquatting       | Dependabot grouped PRs, signed commits required                 |

## Hardening checklist (production)

- [ ] `enterprise.common.jwt.secret` set via Vault / Sealed Secret (≥ 32 bytes random).
- [ ] `X-Gateway-Secret` set, downstream `TrustedGatewayHeaderFilter` enabled.
      In each downstream service add:
      ```yaml
      enterprise:
        common:
          security:
            trusted-gateway:
              enabled: true
              header-name: X-Gateway-Secret
              secret: ${GATEWAY_SHARED_SECRET}
      ```
      and have `api-gateway` inject the same header on every proxied request.
- [ ] Replace H2 with PostgreSQL; enable PITR.
- [ ] Pin all images by digest (`enterprise/...@sha256:...`).
- [ ] Cosign-sign images and verify in admission controller.
- [ ] Enable `NetworkPolicy default-deny-all` (already in `kubernetes/`).
- [ ] Enable mTLS (e.g. via Linkerd / Istio) inside the namespace.
- [ ] Configure log/audit shipping retention ≥ 1 year.
- [ ] Run `./mvnw -Psecurity verify` weekly; fail build on CVSS ≥ 7.
- [ ] Penetration test prior to GA and after major dependency upgrades.

## Incident response

Follow `runbook.md` → "Common incidents". Always:

1. Capture: snapshot logs, metrics, MDC `correlationId` of the offending requests.
2. Contain: `kubectl scale deploy/<svc> --replicas=0` or trip the circuit
   breaker; revoke tokens with `logout/all` if identity is compromised.
3. Eradicate: deploy fix, rotate secrets if the blast radius includes them.
4. Recover: monitor SLOs for one full business cycle.
5. Learn: open an ADR if architecture must change; update this document.
