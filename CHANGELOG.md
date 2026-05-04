# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **GraphQL support** in `common-lib` (toggleable, opt-in):
  - `GraphQlAutoConfiguration` registers extended scalars (`DateTime`,
    `UUID`, `JSON`, `Long`, `BigDecimal`, `Url`, …),
    `MaxQueryDepthInstrumentation` and `MaxQueryComplexityInstrumentation`,
    and the platform exception resolver. Gated on
    `@ConditionalOnClass(spring-boot-starter-graphql)` and
    `enterprise.common.graphql.enabled`.
  - `GraphQlExceptionResolver` maps every platform exception into GraphQL
    errors carrying `extensions.errorCode` from the same `ErrorCode`
    catalog used by REST — clients see one error model across transports.
  - `QueryResponse<T>` envelope: GraphQL-friendly counterpart of
    `ApiResponse<T>` (`status`, `data`, `message`, `correlationId`,
    `timestamp`, `warnings`, `extensions`).
  - All deps `<optional>true</optional>` so REST-only services pay no
    cost.
  - `sample-service` ships a working `schema.graphqls` + resolver and is
    routed through the API gateway at `/graphql`, `/graphiql`.
  - Tests: `QueryResponseTest`, `GraphQlExceptionResolverTest` (12 cases),
    `SampleGraphQlControllerTest` (slice test via `GraphQlTester`).
  - ADR-0005 documents the REST + GraphQL coexistence model.
- **Comprehensive exception handling** in `GlobalExceptionHandler` — now
  covers DB integrity / duplicate / optimistic-lock / pessimistic-lock /
  query-timeout / DB-down / generic SQL, Spring Security
  (`BadCredentials`, `Locked`, `Disabled`, `AccountExpired`,
  `CredentialsExpired`, `AccessDenied`, generic `AuthenticationException`),
  JWT (expired / malformed / signature / unsupported / generic), HTTP
  (415, 406, 413, 408, missing param/header/path-var, multipart,
  async-timeout, `ResponseStatusException`), and JVM-level
  (`IllegalArgument`, `IllegalState`, `UnsupportedOperation`, `Timeout`).
- New typed exceptions: `DuplicateResourceException`, `ConcurrencyException`.
- `ResilienceExceptionAdvice` — conditional `@RestControllerAdvice` mapping
  `CallNotPermittedException` (503), `RequestNotPermitted` (429), and
  `BulkheadFullException` (503) to RFC 7807. Auto-registered via
  `ResilienceExceptionAdviceAutoConfiguration` only when Resilience4j is
  on the classpath.
- Extended `ErrorCode` catalog (24 new entries: `DUPLICATE_RESOURCE`,
  `OPTIMISTIC_LOCK`, `DATABASE_*`, `ACCOUNT_*`, `CREDENTIALS_EXPIRED`,
  `BULKHEAD_FULL`, `MISSING_PARAMETER`, `MISSING_HEADER`, `MALFORMED_JSON`,
  `NOT_ACCEPTABLE`, `PAYLOAD_TOO_LARGE`, `NOT_IMPLEMENTED`, …).
- DB constraint-name extraction helper that surfaces the violated
  constraint under `error.details.constraint`.
- i18n updates: English + German bundles cover every new error key.
- `docs/api.md` ships a complete `ErrorCode` ↔ HTTP catalog and an
  exception → status cheat-sheet.
- Repo hygiene: `LICENSE`, `SECURITY.md`, `CONTRIBUTING.md`,
  `CODE_OF_CONDUCT.md`, `CHANGELOG.md`, `.editorconfig`,
  `.gitattributes`, `.dockerignore`, `docs/adr/`.
- GitHub Actions CI (`build`, `test`, `checkstyle`, `jacoco`,
  `cyclonedx`, `trivy`) and Dependabot configuration.
- Parent POM hardening: SCM, distributionManagement, CycloneDX SBOM,
  OWASP dependency-check, SpotBugs, PMD, license-maven-plugin,
  git-commit-id, Spotless bound to `validate`, build-info goal.
- Per-service multi-stage layered-jar Dockerfiles (non-root,
  `HEALTHCHECK`, Temurin JRE).
- Kubernetes hardening: PodDisruptionBudget, NetworkPolicy, Ingress,
  ServiceAccount + RBAC, Kustomize base + dev/staging/prod overlays,
  startup probes, structured resource limits.
- Observability: `logstash-logback-encoder` JSON logging, common
  Micrometer tags, actuator health groups, git/build info contributor.
- Security upgrades in `auth-service`: `/logout` endpoint, persisted
  refresh-token rotation (Redis), RateLimiter on `/login`, account
  lockout after N failed attempts.
- New `sample-service` module demonstrating gateway routing,
  propagated `X-Auth-*` headers, `ApiResponse`, and idempotency.
- common-lib unit tests for `JwtTokenProvider`, `CorrelationIdFilter`,
  `PayloadMasker`, `IdempotencyAspect`, `AuditAspect`,
  `GlobalExceptionHandler`, `ApiResponse`.
- API gateway WebFlux tests for `JwtAuthenticationGatewayFilter` and a
  trusted-header guard pattern.
- Documentation: ADR template + 0001 (Spring Boot 3 + Java 21), runbook,
  C4 architecture placeholder.

## [1.0.0] - TBD

Initial public release.
