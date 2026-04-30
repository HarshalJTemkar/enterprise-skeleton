# 0005. REST + GraphQL coexistence in `common-lib`

* Status: accepted
* Date: 2026-04-29
* Deciders: platform-team

## Context

Several teams asked for GraphQL support: aggregating multiple REST calls
into one round-trip, evolving APIs without versioning, and avoiding
over-/under-fetching for mobile clients. Replacing REST is not desirable —
the existing REST surface is mature, well-instrumented, and is the
contract used by the API gateway and by partners.

We need a way for services to expose **both** REST and GraphQL with:

* shared identity (the gateway already validates JWTs and forwards
  `X-Auth-Subject` / `X-Auth-Roles`);
* shared **error semantics** (a client-side error catalog that doesn't
  diverge by transport);
* shared **observability** (`correlationId` MDC, Micrometer / OTel);
* shared **envelope shape** so SDKs can deserialize the same DTOs.

## Decision

Add a fully **toggleable** GraphQL feature in `common-lib`:

* `spring-boot-starter-graphql` and `graphql-java-extended-scalars` are
  declared as `<optional>true</optional>` so consumer services that don't
  want GraphQL pay no cost (auto-config is gated on
  `@ConditionalOnClass`).
* `GraphQlAutoConfiguration` registers:
  * Custom scalars: `DateTime`, `Date`, `Time`, `UUID`, `JSON`, `Long`,
    `BigDecimal`, `Url`, `Object`.
  * `MaxQueryDepthInstrumentation` (default 15).
  * `MaxQueryComplexityInstrumentation` (default 200).
  * `GraphQlExceptionResolver` that maps every platform exception to a
    GraphQL error carrying the **same** `ErrorCode` catalog used by
    `GlobalExceptionHandler` for REST. The error code lands under
    `errors[].extensions.errorCode` (plus `classification`,
    `correlationId`, `timestamp`, `details`, `fieldErrors`).
* New envelope `QueryResponse<T>` mirrors `ApiResponse<T>`:
  `status`, `data`, `message`, `correlationId`, `timestamp`, `warnings`,
  `extensions`. Resolvers should return `QueryResponse<T>` so REST and
  GraphQL clients see the same payload shape.
* All knobs live under `enterprise.common.graphql.*` (depth, complexity,
  introspection, scalars, error mapping, expose-message).
* `sample-service` ships a working schema + resolver demonstrating the
  pattern; the gateway forwards `/graphql`, `/graphiql`, `/graphql/**`
  to it via a new route.

## Consequences

* Services opt in by adding `spring-boot-starter-graphql` and a schema —
  no change is required for existing REST-only services.
* Error handling stays consistent: a client receives `errorCode=ERR-1003`
  for `INVALID_CREDENTIALS` regardless of whether the call was REST or
  GraphQL.
* Hardening defaults are conservative: depth & complexity caps on,
  introspection off in production via env override.
* GraphiQL is shipped for development convenience but its path is
  flag-gated (`GRAPHIQL_ENABLED=false` in production overlays).
* Subscriptions (WebSocket) are not enabled in the skeleton; teams that
  need them add `spring-boot-starter-websocket` and configure the
  transport without touching `common-lib`.

## Alternatives considered

* **Federated GraphQL gateway (Apollo / federation-jvm)** — overkill at
  this scale; a single-service GraphQL endpoint is sufficient. We left
  the door open: switching to federation only requires adding the
  federation-jvm dependency and emitting `_service { sdl }`.
* **GraphQL replacing REST** — rejected: REST is the partner-facing
  contract, supports streaming downloads, file uploads, and the
  RFC 7807 ProblemDetail integration with the existing client SDKs.
* **Custom non-Spring runtime (graphql-java directly)** — rejected:
  Spring GraphQL handles tracing, security, validation,
  `@SchemaMapping`, `@BatchMapping`, and slice tests out of the box.
