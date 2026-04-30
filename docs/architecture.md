# Architecture

> Living document. For decisions see [`adr/`](adr/).

## C4 — System context (level 1)

```mermaid
flowchart LR
    User([Web / Mobile / 3rd-party client])
    subgraph Internet
      User
    end

    subgraph Edge
      LB[Cloud Load Balancer + TLS]
      Ingress[NGINX Ingress]
    end

    subgraph Platform [enterprise-skeleton (k8s namespace)]
      GW[api-gateway<br/>Spring Cloud Gateway, WebFlux]
      Auth[auth-service<br/>Spring Boot MVC]
      Sample[sample-service<br/>Spring Boot MVC]
      Reg[service-registry<br/>Eureka]
      Cfg[config-server<br/>Spring Cloud Config]
      DB[(PostgreSQL)]
      Redis[(Redis)]
    end

    subgraph Observability
      Prom[Prometheus]
      Otel[OTel Collector]
      Graf[Grafana]
    end

    User --> LB --> Ingress --> GW
    GW -- lb://auth-service --> Auth
    GW -- lb://sample-service --> Sample
    Auth --> DB
    Auth --> Redis
    Sample --> Redis
    Auth -.register.-> Reg
    Sample -.register.-> Reg
    GW -.discover.-> Reg
    Auth -.config.-> Cfg
    Sample -.config.-> Cfg
    GW -.config.-> Cfg
    Auth -- /actuator/prometheus --> Prom
    Sample -- /actuator/prometheus --> Prom
    GW -- /actuator/prometheus --> Prom
    Auth --> Otel
    Sample --> Otel
    GW --> Otel
    Otel --> Graf
    Prom --> Graf
```

## C4 — Container view (level 2)

| Container          | Tech                     | Responsibility                                              | Stateful? |
|--------------------|--------------------------|-------------------------------------------------------------|-----------|
| `api-gateway`      | Spring Cloud Gateway     | TLS termination passthrough, JWT validation, header inject  | no        |
| `auth-service`     | Spring Boot 3 (MVC)      | Login, refresh-token rotation, logout, lockout              | yes (DB)  |
| `sample-service`   | Spring Boot 3 (MVC)      | Demonstrates downstream consumption of platform contracts   | no        |
| `service-registry` | Eureka                   | Client-side service discovery                               | no        |
| `config-server`    | Spring Cloud Config      | Centralized externalized configuration                      | no        |
| `common-lib`       | Library JAR              | Auto-config: i18n, logging, security, idempotency, audit    | n/a       |

## Cross-cutting capabilities (provided by `common-lib`)

* `ApiResponse<T>` envelope + RFC-7807 ProblemDetails
* Correlation-ID + Request-ID + payload masking + JSON logging
* `@Idempotent` (Redis) + `@Auditable` (AOP)
* Resilience4j (CB / Retry / RateLimiter / TimeLimiter / Bulkhead)
* JWT helpers + `TrustedGatewayHeaderFilter` (zero-trust downstream guard)
* OpenTelemetry tracing + Micrometer / Prometheus metrics
* Caffeine + Redis cache, JPA auditing, Flyway activation

## Request lifecycle

1. Client → NGINX Ingress → `api-gateway`.
2. `JwtAuthenticationGatewayFilter` validates JWT (HS256), forwards
   `X-Auth-Subject` + `X-Auth-Roles` + `X-Correlation-ID`.
3. Downstream service runs `CorrelationIdFilter` → `TrustedGatewayHeaderFilter`
   (prod) → controllers; logs and metrics inherit MDC.
4. Response envelope = `ApiResponse<T>` for success, `ProblemDetail` for failures.

## Failure modes & guard rails

| Risk                              | Mitigation                                             |
|-----------------------------------|--------------------------------------------------------|
| Brute-force login                 | RateLimiter (`auth-login`) + per-user lockout (V3)     |
| Stolen JWT                        | Short access TTL + refresh-token rotation + `/logout`  |
| Replayed refresh token            | Re-use detection revokes all tokens for the user       |
| Direct call bypasses gateway      | `TrustedGatewayHeaderFilter` (shared secret)           |
| Spoofed identity headers          | Stripped before logging by trusted-gateway filter      |
| Cascading failure                 | Resilience4j circuit breaker on outbound RestClient    |
| Noisy neighbour                   | Bulkhead + virtual-thread executor with bounded queue  |
| Log leakage of secrets            | `PayloadMasker` (configurable JSON / form / Bearer)    |
| Unbounded refresh-token table     | Daily `@Scheduled` purge of expired rows               |

## Environments

| Env       | Profile     | Replicas | Tracing | Logging |
|-----------|-------------|----------|---------|---------|
| local-dev | `dev`       | 1        | off     | text    |
| staging   | `staging`   | 2        | on      | json    |
| prod      | `prod`      | 3+       | on      | json    |

Apply with `kubectl apply -k kubernetes/overlays/<env>`.
