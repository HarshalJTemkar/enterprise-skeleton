# Enterprise Skeleton

Production-ready multi-module starter built on **Java 21**, **Spring Boot 3.4**, and
**Spring Cloud 2024**. Every feature is toggleable via configuration (profiles,
environment variables, or Config Server).

```
enterprise-skeleton/
├── pom.xml              # Parent POM (BOMs, plugins, profiles, enforcer)
├── common-lib/          # Shared JAR (DTOs, exceptions, JWT helper, auto-config)
├── config-server/       # Spring Cloud Config Server
├── service-registry/    # Netflix Eureka Server
├── api-gateway/         # Spring Cloud Gateway + reactive JWT filter
├── auth-service/        # JWT-based authentication service
├── sample-service/      # Demo downstream service routed via the gateway
├── kubernetes/          # Manifests + Kustomize base + dev/staging/prod overlays
├── docker/              # Compose, observability stack
├── docs/                # Architecture, runbook, security, ADRs
└── .github/             # CI, CodeQL, Dependabot, PR template
```

## Documentation

| Topic              | File                                                  |
|--------------------|-------------------------------------------------------|
| API reference      | [`docs/api.md`](docs/api.md)                          |
| Architecture (C4)  | [`docs/architecture.md`](docs/architecture.md)        |
| On-call runbook    | [`docs/runbook.md`](docs/runbook.md)                  |
| Security model     | [`docs/security.md`](docs/security.md)                |
| Decision records   | [`docs/adr/`](docs/adr/)                              |

---

## Highlights

### Parent POM
- Multi-module Maven management with centralized versions
- **BOM imports**: Spring Boot, Spring Cloud, Testcontainers, Resilience4j
- Java 21 `<release>` compiler config, `-parameters`, strict `-Xlint:all`
- Annotation processors wired: **Lombok**, **MapStruct**, Spring Config Processor
- Plugin management: **Surefire**, **Failsafe**, **Checkstyle**, **Jacoco**,
  **Spotless**, Spring Boot Maven Plugin
- **Profiles**: `dev` (default, relaxed), `staging`, `prod` (sources + javadoc),
  plus a `coverage` profile that enforces 70% line coverage
- **Enforcer rules**: require Java 21+, Maven 3.9+, dependency convergence,
  ban duplicate POM dependency versions

### Virtual Threads (Java 21)
Enabled globally via `spring.threads.virtual.enabled=true`. Controlled by the
parent-pom property `spring.threads.virtual.enabled` and overridable per-env.

### common-lib
- `ApiResponse<T>` canonical envelope (`success`, `data`, `error`, `traceId`)
- `BusinessException` / `ResourceNotFoundException`
- `GlobalExceptionHandler` (opt-in via
  `enterprise.common.web.exception-handler.enabled`)
- `JwtTokenProvider` (opt-in via `enterprise.common.security.enabled=true`)
- Spring Boot 3 auto-configuration via
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- All behavior driven by `CommonProperties` (prefix `enterprise.common.*`)

### config-server
- Native profile for local dev (`classpath:/config`) and Git profile for prod
- HTTP basic auth, actuator endpoints
- Eureka client (toggleable)

### service-registry
- Netflix Eureka server with basic auth, self-preservation tunable

### api-gateway
- Spring Cloud Gateway (reactive) with **discovery-based routing**
- `JwtAuthenticationGatewayFilter` validates JWTs and forwards
  `X-Auth-Subject` / `X-Auth-Roles` to downstream services
- `gateway.security.jwt.public-paths` configures public endpoints
- Global CORS, resilience4j circuit breaker starter included

### auth-service
- Stateless JWT login & refresh-token endpoints
- In-memory users configurable via `auth.users[*]` (swap to DB in prod)
- SpringDoc OpenAPI / Swagger UI at `/swagger-ui.html`

---

## Build & Run

### Prerequisites
- JDK **21** (Temurin recommended)
- Maven **3.9+**
- Docker / Docker Compose (optional)

### Build everything
```bash
./mvnw -Pdev clean package
# or for a release build with source+javadoc jars:
./mvnw -Pprod clean verify
```

### Run locally (one terminal per service)
```bash
java -jar service-registry/target/service-registry.jar
java -jar config-server/target/config-server.jar
java -jar auth-service/target/auth-service.jar
java -jar api-gateway/target/api-gateway.jar
```

### Run with Docker Compose
```bash
./mvnw -Pdev clean package -DskipTests
cd docker
cp .env.example .env
docker compose up --build
```

Services:
| Service          | URL                          |
|------------------|------------------------------|
| Eureka           | http://localhost:8761        |
| Config Server    | http://localhost:8888        |
| API Gateway      | http://localhost:8080        |
| Auth (direct)    | http://localhost:9000        |
| Swagger (auth)   | http://localhost:9000/swagger-ui.html |

### Quick JWT smoke test
```bash
# login through the gateway
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

---

## Maven profiles

| Profile    | Purpose                                        |
|------------|------------------------------------------------|
| `dev`      | Default. Skips checkstyle & enforcer for speed |
| `staging`  | Full quality gates, non-release artifacts      |
| `prod`     | Quality gates + source/javadoc jars            |
| `coverage` | Enforces ≥ 70% line coverage via Jacoco        |

Activate with `-P<profile>`; compose multiple: `-Pstaging,coverage`.

---

## Configuration reference (prefix `enterprise.common`)

| Property                                                 | Default                         |
|----------------------------------------------------------|---------------------------------|
| `enterprise.common.web.exception-handler.enabled`        | `true`                          |
| `enterprise.common.web.cors.enabled`                     | `false`                         |
| `enterprise.common.security.enabled`                     | `false`                         |
| `enterprise.common.jwt.secret`                           | *(override via env)*            |
| `enterprise.common.jwt.access-token-ttl`                 | `PT1H`                          |
| `enterprise.common.jwt.refresh-token-ttl`                | `P7D`                           |
| `enterprise.common.jwt.issuer`                           | `enterprise-platform`           |
| `enterprise.common.jwt.header-name`                      | `Authorization`                 |
| `enterprise.common.jwt.token-prefix`                     | `"Bearer "`                     |
| `spring.threads.virtual.enabled`                         | `true`                          |
| `gateway.security.jwt.enabled`                           | `true`                          |
| `gateway.security.jwt.public-paths`                      | *(list)*                        |

---

## common-lib capabilities (all opt-in / fully configurable)

Every feature below is implemented as a Spring Boot 3 auto-configuration
(registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`)
and can be disabled with a single property.

| Capability       | Prefix                                          | Master switch | Key classes |
|------------------|-------------------------------------------------|---------------|-------------|
| Swagger / OpenAPI | `enterprise.common.openapi.*`                  | `enabled`     | `OpenApiAutoConfiguration`, `OpenApiProperties` |
| MapStruct mappers | (compile-time)                                 | —             | `EnterpriseMapperConfig`, `BaseMapper<E,D>` |
| CircuitBreaker    | `enterprise.common.resilience.circuit-breaker.*` | `enabled`   | `ResilienceAutoConfiguration` |
| RateLimiter       | `enterprise.common.resilience.rate-limiter.*`  | `enabled`     | `ResilienceAutoConfiguration` |
| TimeLimiter       | `enterprise.common.resilience.time-limiter.*`  | `enabled`     | `ResilienceAutoConfiguration` |
| Retry             | `enterprise.common.resilience.retry.*`         | `enabled`     | `ResilienceAutoConfiguration` |
| Bulkhead          | `enterprise.common.resilience.bulkhead.*`      | `enabled`     | `ResilienceAutoConfiguration` |
| `@Async` executor | `enterprise.common.async.*`                    | `enabled`     | `AsyncAutoConfiguration`, `AsyncProperties` |
| Parallel fan-out  | `enterprise.common.async.parallel.*`           | `enabled`     | `ParallelExecutor` |
| `@Scheduled`      | `enterprise.common.scheduler.*`                | `enabled`     | `SchedulerAutoConfiguration`, `SchedulerProperties` |

All defaults live in **`config-server/src/main/resources/config/application.yml`**
and are served to every service; override per-service in the corresponding
`<service>.yml` file or via environment variables.

### Using the features in a service

**Swagger** — add `springdoc-openapi-starter-webmvc-ui` to the module POM.
Set `enterprise.common.openapi.title`, `description`, etc. Swagger UI is live
at `/swagger-ui.html`.

**MapStruct** — declare a mapper:
```java
@Mapper(config = EnterpriseMapperConfig.class)
public interface UserMapper extends BaseMapper<UserEntity, UserDto> {}
```

**Resilience4j** — add the starters shown in `auth-service/pom.xml`, then use
the shared `"default"` instance on any method:
```java
@CircuitBreaker(name = "default", fallbackMethod = "fallback")
@RateLimiter(name = "default")
@Retry(name = "default")
@Bulkhead(name = "default")
@TimeLimiter(name = "default")       // must return CompletionStage / CompletableFuture
public CompletableFuture<String> call() { ... }
```

**`@Async`** — just annotate; `AsyncAutoConfiguration` enables `@EnableAsync`
and registers a virtual-thread-per-task `taskExecutor` by default.

**Parallel calls** — inject `ParallelExecutor`:
```java
List<Invoice> out = parallelExecutor.map(ids, id -> billingClient.fetch(id));
parallelExecutor.map(ids, mapper, 2_000);  // with 2s overall timeout
parallelExecutor.runAll(List.of(r1, r2, r3));
```

**Scheduling** — annotate with `@Scheduled(cron = "${my.job.cron}")` or
`@Scheduled(fixedDelayString = "${my.job.delay}")`. The scheduler runs on
virtual threads when `enterprise.common.scheduler.use-virtual-threads=true`.

### Demo endpoints (auth-service)

All endpoints exposed publicly under `/demo/**` for quick verification:

| Method | Path                          | Feature illustrated              |
|--------|-------------------------------|----------------------------------|
| GET    | `/demo/circuit-breaker`       | `@CircuitBreaker` + fallback     |
| GET    | `/demo/rate-limiter`          | `@RateLimiter`                   |
| GET    | `/demo/retry`                 | `@Retry` + exponential backoff   |
| GET    | `/demo/bulkhead`              | `@Bulkhead`                      |
| GET    | `/demo/async-timelimiter?delayMs=5000` | `@TimeLimiter` + `@Async`  |
| GET    | `/demo/parallel?tasks=10`     | `ParallelExecutor.map`           |
| GET    | `/demo/fire-and-forget`       | `@Async` fire-and-forget         |
| GET    | `/demo/mapper`                | MapStruct entity → DTO           |

Browse `http://localhost:9000/swagger-ui.html` to try them from the browser.

---

## Phase 1 – common-lib full feature matrix

Every feature is implemented as Spring Boot 3 auto-configuration, registered
in `common-lib/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`,
and is independently toggleable.

### Exception & Error Handling

| Item | Class |
|---|---|
| Typed hierarchy | `BaseException` → `BusinessException`, `ValidationException`, `IntegrationException`, `SecurityAppException`, `ResourceNotFoundException` |
| Error catalog | `ErrorCode` enum (code + i18n key + HTTP status) |
| Global advice (RFC 7807) | `GlobalExceptionHandler` (returns `application/problem+json` with embedded `ErrorResponse` envelope) |
| Field-level violations | `ValidationException.FieldViolation` collected by `Builder` |
| i18n-aware messages | resolved via `MessageService` from `Accept-Language` |

Toggle with `enterprise.common.web.exception-handler.enabled=true|false`.

### Internationalization (i18n)

| Item | Class |
|---|---|
| MessageSource (reloadable) | `I18nAutoConfiguration` |
| Locale resolver | `AcceptHeaderLocaleResolver` |
| Programmatic facade | `MessageService.get(key, args...)` |
| Default bundles | `i18n/messages.properties`, `messages_de.properties` |

Drop a new `messages_xx.properties` to add a language. Configure with
`enterprise.common.i18n.*`.

### Logging & Correlation

| Item | Class |
|---|---|
| Correlation-ID filter | `CorrelationIdFilter` (reads/echoes `X-Correlation-ID`, stores in MDC) |
| Request/response access log | `RequestResponseLoggingFilter` (configurable body capture, exclude patterns) |
| PII / secret masking | `PayloadMasker` (configurable JSON field list + Bearer-token regex) |
| Per-module log level | `logging.level.com.enterprise.<module>=DEBUG` |
| Structured JSON logs | include `logback-common.xml`; switch with `LOG_STYLE=json` |

Toggle with `enterprise.common.logging.{correlation-id|access-log|masking}.enabled`.

### Audit Framework

| Item | Class |
|---|---|
| Annotation | `@Auditable(action, resource, idExpression, captureArgs, captureResult)` |
| Event | `AuditEvent` record (who/what/when/correlationId/before/after/success) |
| Aspect | `AuditAspect` (AOP) |
| Default sink | `AUDIT` SLF4J logger (replace bean to ship to DB / Kafka) |

Toggle with `feature.audit.enabled=true|false`.

### Idempotency

| Item | Class |
|---|---|
| Annotation | `@Idempotent(name, ttl)` |
| Header | `Idempotency-Key` (configurable) |
| Store | `RedisIdempotencyStore` (`SET NX EX`) |
| Aspect | `IdempotencyAspect` (replays cached response on duplicate) |

Toggle with `feature.idempotency.enabled` (auto-disabled when Redis is absent).

### API Response Wrappers

| Type | Purpose |
|---|---|
| `ApiResponse<T>` | success envelope with status / data / message / correlationId / timestamp |
| `PagedResponse<T>` | paginated list + `Pagination` metadata, with `fromSpringPage()` adapter |
| `ErrorResponse` | structured error body embedded in `ProblemDetail` |

### Security Utilities

| Item | Class |
|---|---|
| `PasswordEncoder` bean | `PasswordEncoderConfig` (BCrypt or Spring delegating, configurable strength) |
| Static helper | `SecurityContextHelper.currentUsername() / hasRole(..)` |
| JWT helper | `JwtTokenProvider` (already wired, reused by gateway + auth-service) |

### Base Domain

| Class | Description |
|---|---|
| `BaseEntity` | `id`, `version`, `createdAt/By`, `updatedAt/By` (Spring Data JPA auditing) |
| `AuditableEntity` | adds `AppStatus` |
| `SoftDeletableEntity` | `deleted`, `deletedAt`, `deletedBy` + `softDelete(actor)` |
| `BaseDto` | marker interface for DTOs |

### Common Utilities

| Class | Highlights |
|---|---|
| `DateTimeUtils` | ISO-UTC formatter, epoch helpers, `startOfDayUtc / endOfDayUtc` |
| `StringUtils` | `mask`, `slugify`, `normalizeWhitespace`, `sanitizeForLog` |
| `PaginationUtils` | safe `Pageable` builder with hard cap |
| `JsonUtils` | shared, lenient `ObjectMapper` (Java Time on, no exceptions thrown) |
| `CryptoUtils` | AES-GCM encrypt/decrypt, HMAC-SHA256 hex, URL-safe Base64 |
| `ValidationUtils` | regexes for email / E.164 phone / IBAN / UUID |
| `@PhoneNumber`, `@Iban` | JSR-380 custom constraints |

### Common Enums

`AppStatus`, `Environment`, `AuditAction`, `SortDirection`, plus constants
`ContentType` and `HttpHeaderConstants`.

### Health & Actuator

| Item | Class |
|---|---|
| Custom health template | `BaseHealthIndicator` (exception-safe) |
| `/actuator/info` contributor | `ApplicationInfoContributor` (name, profile, java, build version, git commit) |
| Probes | liveness / readiness groups configured in service YAML |

---

## Phase 2A – service-registry (Eureka)

`service-registry/src/main/resources/application.yml` now ships **two
profiles**:

- **`standalone`** (default) — single-node Eureka, no self-registration.
- **`peer`** — HA replication. Provide the peer URLs at startup:

```bash
EUREKA_PROFILE=peer \
EUREKA_HOSTNAME=node1 \
EUREKA_PEER_URLS=http://eureka:eureka@node2:8761/eureka/,http://eureka:eureka@node3:8761/eureka/ \
java -jar service-registry/target/service-registry.jar
```

Tunables (every value comes from env / config-server):

| Property | Env override | Default |
|---|---|---|
| Self-preservation | `EUREKA_SELF_PRESERVATION` | `true` |
| Renewal threshold | `EUREKA_RENEWAL_THRESHOLD` | `0.85` |
| Eviction interval (ms) | `EUREKA_EVICTION_INTERVAL_MS` | `10000` |
| Cache update (ms) | `EUREKA_CACHE_UPDATE_MS` | `5000` |
| Registry fetch (s) | `EUREKA_REGISTRY_FETCH_INTERVAL` | `30` |
| Zone metadata | `EUREKA_ZONE` | `primary` |
| Basic-auth user | `EUREKA_USER` / `EUREKA_PASSWORD` | `eureka` / `eureka` |

The Docker Compose service uses a Spring Boot **liveness probe** healthcheck
(`/actuator/health/liveness`) with a 30s start-period — services that depend
on Eureka wait until it reports `UP`.

---

## Phase P1 / P2 / P3 additions

### 🔴 P1 – Production must-haves

| Item | Class / file | Toggle |
|---|---|---|
| CORS filter | `web/CorsAutoConfiguration` | `enterprise.common.web.cors.enabled` |
| Locale-change interceptor (`?lang=de`) | `web/WebMvcLocaleConfig` | `enterprise.common.i18n.enabled` |
| JPA auditing (`createdBy`/`updatedBy`) | `audit/JpaAuditingConfig`, `audit/CurrentAuditorAware` | `enterprise.common.jpa.auditing.enabled` |
| Per-service JWT validation filter | `security/JwtAuthenticationFilter`, `security/ResourceServerAutoConfiguration` | `enterprise.common.security.resource-server.enabled` |
| ProblemDetail entry-point + access-denied handler | `security/ProblemDetailAuthenticationEntryPoint` | (registered with the resource-server filter chain) |
| `JwtTokenProvider` bean (was `@Component`) | `security/JwtAutoConfiguration` | `enterprise.common.security.enabled` |
| Validation messages bundle | `ValidationMessages.properties` / `_de.properties` | (Hibernate Validator default) |
| `i18n/errors.properties` placeholder | fixes startup warning | — |

### 🟠 P2 – Foundational platform features

| Item | Class / file | Toggle |
|---|---|---|
| OpenTelemetry tracing | `observability/TracingAutoConfiguration` + `management.tracing.*` | `enterprise.common.observability.tracing.enabled` |
| Resilient `RestClient` (correlation/tenant header propagation) | `http/ResilientRestClientConfig` | `enterprise.common.http.client.enabled` |
| Spring Cache (Caffeine ↔ Redis) | `cache/CacheAutoConfiguration` (+ nested `RedisCache`) | `enterprise.common.cache.enabled`, `…cache.type=caffeine|redis` |
| Flyway activation marker | `persistence/FlywayActivation` | `enterprise.common.flyway.enabled` |
| Gateway hardening | `api-gateway/application.yml` + `gateway/config/RateLimiterConfig`, `gateway/controller/FallbackController` | `redis-rate-limit` profile |
| auth-service JPA users + roles | `auth/user/{UserEntity,RoleEntity,UserRepository,RoleRepository,JpaUserDetailsService}` + Flyway `V1__init.sql`, `V2__seed_users.sql` | (always on; H2 default, override `DB_URL`/`DB_USER`/`DB_PASSWORD`) |

### 🟡 P3 – Test, ops & K8s

- **Test fixtures** (shipped in `common-lib` test-jar):
  - `@WithMockJwtUser(username, subject, roles)` + `WithMockJwtUserSecurityContextFactory`
  - `PostgresTestContainer.getInstance()` (singleton, `withReuse(true)`)
  - `RedisTestContainer.getInstance()`
- **Sample tests in auth-service**:
  - `AuthServiceApplicationTests` – context loads, Flyway seeds `admin`/`user`
  - `DemoControllerTest` – exercises `/demo/mapper` with `@WithMockJwtUser`
  - `application-test.yml` – disables Eureka/Config-Server for fast tests
- **Observability stack** (`docker/docker-compose.observability.yml`):
  - Prometheus (scrapes every service via `prometheus.yml`)
  - Grafana (auto-provisioned datasource)
  - OpenTelemetry Collector (`otel-collector.yaml`, OTLP gRPC/HTTP)
  - Start with: `docker compose -f docker-compose.yml -f docker-compose.observability.yml up`
- **Kubernetes manifests** (`kubernetes/`):
  - `00-namespace.yaml`, `10-configmap.yaml` (ConfigMap + Secret)
  - `20-service-registry.yaml` (StatefulSet + Service + auth-protected probes)
  - `30-config-server.yaml`, `40-auth-service.yaml` (+ HPA), `50-api-gateway.yaml` (+ optional Ingress)

### Test command cheatsheet

```bash
./mvnw -Pdev verify                              # whole reactor
./mvnw -Pdev -pl auth-service -am test           # just one module + deps
./mvnw -Pcoverage verify                         # enforce ≥ 70 % line coverage
```

```text
[INFO] BUILD SUCCESS
[INFO] enterprise-skeleton . SUCCESS
[INFO] common-lib .......... SUCCESS
[INFO] config-server ....... SUCCESS
[INFO] service-registry .... SUCCESS
[INFO] api-gateway ......... SUCCESS
[INFO] auth-service ........ SUCCESS  (Tests run: 2, Failures: 0, Errors: 0)
```