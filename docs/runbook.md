# Operational Runbook

Audience: on-call engineers and platform operators.

## Health & smoke

```bash
# Container probes
kubectl -n enterprise get pods
kubectl -n enterprise port-forward svc/api-gateway 8080:80
curl -fsS http://localhost:8080/actuator/health

# End-to-end smoke (needs JWT_SECRET to be production value)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

## Common incidents

### 1. `5xx` rate spiking

```bash
# Look at the per-service error budget
curl -s http://prometheus/api/v1/query \
  --data-urlencode 'query=sum by (application)(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))'

# Identify the offending route
kubectl -n enterprise logs -l app=api-gateway --tail=500 | jq 'select(.level=="ERROR")'
```

Fast remediation:

* Roll back the last deploy: `kubectl -n enterprise rollout undo deploy/<svc>`.
* Trip the circuit breaker manually:
  `curl -XPOST http://<pod>:9000/actuator/circuitbreakers/default/transition?state=OPEN`.

### 2. Brute-force / credential stuffing on `/login`

* Per-user lockout fires after `auth.lockout.max-attempts` (default 5) for
  `auth.lockout.duration` (default 15 min). To unlock manually:
  ```sql
  UPDATE users SET failed_login_attempts=0, locked_until=NULL WHERE username='alice';
  ```
* Tighten the global limiter:
  ```bash
  kubectl -n enterprise set env deploy/auth-service AUTH_LOGIN_RPS=2
  ```

### 3. JWT secret rotation

1. Generate a new 32+ byte secret.
2. Update the `enterprise-platform-secrets` Secret (or Vault path).
3. Restart **api-gateway and all services** in this order:
   `auth-service → sample-service → api-gateway`.
4. Existing access tokens become invalid; clients must call `/login` again.
   Refresh tokens are invalidated implicitly because the new key cannot
   verify them.

### 4. Refresh-token compromise (specific user)

```sql
UPDATE refresh_tokens SET revoked = true WHERE username = 'alice';
```
Or hit `POST /api/auth/logout/all` while authenticated as the user.

### 5. Config Server unreachable

Services boot with `optional:configserver:` so they fall back to bundled
`application.yml`. Check:

```bash
kubectl -n enterprise logs deploy/config-server | tail
curl -u admin:admin http://config-server:8888/actuator/health
```

### 6. Eureka shows DOWN instances after rolling deploy

* Increase `eureka.client.registryFetchIntervalSeconds` if propagation lag.
* Verify each pod can reach `EUREKA_SERVICE_URL`.
* Disable self-preservation **only** in non-prod for faster eviction.

### 7. Prometheus scrape failures

* Pod missing the `prometheus.io/scrape: "true"` annotation?
* `NetworkPolicy` `allow-prometheus-scrape` requires Prometheus to live in
  the `observability` namespace (label
  `kubernetes.io/metadata.name=observability`).

## Routine maintenance

| Task                           | When     | How                                                       |
|--------------------------------|----------|-----------------------------------------------------------|
| Refresh-token table cleanup    | daily    | `RefreshTokenService.purgeExpired` (`0 0 3 * * *` cron)   |
| Dependency security scan       | nightly  | GitHub Actions `dependency-check` job (CI)                |
| Image scan                     | nightly  | GitHub Actions `trivy-fs` + `docker-images` job           |
| Rotate JWT secret              | quarterly| Section "JWT secret rotation" above                       |
| Rotate `X-Gateway-Secret`      | quarterly| Update Secret + restart all services                      |
| Database backup verification   | weekly   | Restore latest snapshot to staging and run smoke tests    |

## Disaster recovery (DR)

* **Auth DB** — point-in-time recovery (PITR). RPO = 5 min, RTO = 30 min.
* **Eureka / Config Server** — stateless; rebuild from manifests in < 5 min.
* **Secrets** — sourced from Vault / Sealed Secrets; restore namespace from
  last good GitOps commit.

## Useful one-liners

```bash
# Tail JSON logs from one service via stern
stern -n enterprise auth-service --output raw | jq -r 'select(.level!="DEBUG")'

# Show top-N slow endpoints in last hour
curl -sG http://prometheus/api/v1/query \
  --data-urlencode 'query=topk(10, histogram_quantile(0.95, sum by (uri,le)(rate(http_server_requests_seconds_bucket[1h]))))'

# Force a Spring Cloud Config refresh without restart
curl -XPOST http://auth-service:9000/actuator/refresh
```
