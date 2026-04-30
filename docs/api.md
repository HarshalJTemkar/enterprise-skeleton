# API Reference

Complete catalog of every HTTP endpoint exposed by `enterprise-skeleton`.
External clients should always go through the **API gateway** (`:8080`);
direct ports are listed for in-cluster / debug use only.

> Conventions
> * Success body: [`ApiResponse<T>`](apiresponset-success-envelope) envelope.
> * Failure body: RFC 7807 [`ProblemDetail`](problemdetail-error-envelope).
> * Auth: `Authorization: Bearer <accessToken>` (HS256 JWT).
> * Correlation: every request/response carries `X-Correlation-ID`.
> * Idempotency: send `Idempotency-Key: <uuid>` on `@Idempotent` POSTs.

---

## 1. Service map

| Port  | Service           | Public URL via gateway              | Direct (debug only)        |
|-------|-------------------|-------------------------------------|----------------------------|
| 8080  | `api-gateway`     | `http://localhost:8080`             | —                          |
| 9000  | `auth-service`    | `/api/auth/**` (StripPrefix=1)      | `http://localhost:9000`    |
| 9100  | `sample-service`  | `/api/v1/sample/**` (REST)<br/>`/graphql`, `/graphiql` (GraphQL) | `http://localhost:9100`    |
| 8761  | `service-registry`| not exposed                         | `http://localhost:8761`    |
| 8888  | `config-server`   | not exposed                         | `http://localhost:8888`    |

---

## 2. `auth-service`

Base path through the gateway: **`/api/auth`** (`StripPrefix=1`).
Direct base path: **`/`**.

| # | Method | Path (gateway / direct)                  | Auth         | Rate-limited | Idempotent |
|---|--------|------------------------------------------|--------------|--------------|------------|
| 1 | POST   | `/api/auth/login`     `/login`           | none         | yes (`auth-login`) | no         |
| 2 | POST   | `/api/auth/refresh`   `/refresh`         | refresh JWT  | no                  | no         |
| 3 | POST   | `/api/auth/logout`    `/logout`          | refresh JWT  | no                  | yes (DB)   |
| 4 | POST   | `/api/auth/logout/all` `/logout/all`     | access JWT   | no                  | yes (DB)   |
| 5 | GET    | `/demo/circuit-breaker`                  | none         | —                   | no         |
| 6 | GET    | `/demo/rate-limiter`                     | none         | yes (`default`)     | no         |
| 7 | GET    | `/demo/retry`                            | none         | —                   | no         |
| 8 | GET    | `/demo/bulkhead`                         | none         | —                   | no         |
| 9 | GET    | `/demo/async-timelimiter?delayMs={n}`    | none         | —                   | no         |
| 10| GET    | `/demo/parallel?tasks={n}`               | none         | —                   | no         |
| 11| GET    | `/demo/fire-and-forget`                  | none         | —                   | no         |
| 12| GET    | `/demo/mapper`                           | none         | —                   | no         |
| 13| GET    | `/v3/api-docs`, `/swagger-ui.html`       | none         | —                   | no         |

### 2.1 `POST /login`

Authenticates a user and issues an access + refresh token pair.

* Headers: `Content-Type: application/json`
* Rate limiter: `auth-login` (default 10 r/s, configurable via `AUTH_LOGIN_RPS`).
* Lockout: after `auth.lockout.max-attempts` consecutive failures (default 5)
  the account is locked for `auth.lockout.duration` (default 15 min).

Request:
```json
{ "username": "admin", "password": "admin" }
```

Response `200 OK`:
```json
{
  "status": "success",
  "data": {
    "accessToken":  "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
    "tokenType":    "Bearer",
    "expiresInSeconds": 3600
  },
  "correlationId": "abc-123",
  "timestamp": "2026-04-27T10:00:00Z"
}
```

Errors:
| Status | code                  | When                                     |
|--------|-----------------------|------------------------------------------|
| 400    | `VALIDATION_FAILED`   | blank username or password               |
| 401    | `INVALID_CREDENTIALS` | wrong username/password                  |
| 423    | `ACCOUNT_LOCKED`      | too many failed attempts                 |
| 429    | `TOO_MANY_REQUESTS`   | global RateLimiter exhausted             |

Curl:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

### 2.2 `POST /refresh`

Rotates the refresh token. The supplied token is **revoked** and a new
access + refresh pair is returned. Re-using a revoked token revokes **all**
the user's tokens (replay detection).

Request:
```json
{ "refreshToken": "eyJhbGciOi..." }
```

Response `200 OK`: same shape as `/login`.

Errors:
| Status | code            | When                                |
|--------|-----------------|-------------------------------------|
| 400    | `VALIDATION_FAILED` | blank `refreshToken`            |
| 401    | `TOKEN_INVALID` | unknown / expired / revoked / forged|

### 2.3 `POST /logout`

Revokes a single refresh token. Always returns `204 No Content` (idempotent
even if the token is already revoked or unknown — by design, to avoid
information leakage).

Request:
```json
{ "refreshToken": "eyJhbGciOi..." }
```

### 2.4 `POST /logout/all`

Revokes every active refresh token for the authenticated principal.

* Auth: `Authorization: Bearer <accessToken>` (must be valid).
* Response: `204 No Content`.

### 2.5 Demo endpoints (`/demo/**`)

All public, all return `ApiResponse<T>`. Intended for the Swagger UI.

| Endpoint                        | Sample response (`data`)                              |
|---------------------------------|--------------------------------------------------------|
| `GET /demo/circuit-breaker`     | `"ok"` or fallback `"fallback: …"`                     |
| `GET /demo/rate-limiter`        | `"ok"` or `429` once limit exceeded                    |
| `GET /demo/retry`               | `"ok-after-retries:N"`                                 |
| `GET /demo/bulkhead`            | `"ok"`                                                 |
| `GET /demo/async-timelimiter?delayMs=500` | `"ok-after-500ms"` (or fallback if > limit)  |
| `GET /demo/parallel?tasks=5`    | `["task-1@VirtualThread", …]`                          |
| `GET /demo/fire-and-forget`     | `"submitted-on-VirtualThread[#…]"`                     |
| `GET /demo/mapper`              | `{ "id":1, "username":"demo", "roles":[…] }`           |

---

## 3. `sample-service`

Base path through the gateway: **`/api/v1/sample`** (no strip).
Direct base path: **`/api/v1/sample`**.

| # | Method | Path                          | Auth (gateway-injected) | Idempotent |
|---|--------|-------------------------------|-------------------------|------------|
| 1 | GET    | `/api/v1/sample/whoami`       | bearer                  | no         |
| 2 | POST   | `/api/v1/sample/orders`       | bearer                  | yes        |

The service does **not** validate JWTs itself. The gateway forwards
`X-Auth-Subject` / `X-Auth-Roles` after validating the token; in production
enable `TrustedGatewayHeaderFilter` (common-lib) to reject calls bypassing
the gateway with `403`.

### 3.1 `GET /api/v1/sample/whoami`

Echoes the gateway-propagated identity headers.

Response `200 OK`:
```json
{
  "status": "success",
  "data": {
    "subject": "alice",
    "roles":   "[ROLE_USER, ROLE_ADMIN]",
    "correlationId": "abc-123"
  }
}
```

### 3.2 `POST /api/v1/sample/orders`

Creates an order. **Idempotent** via `@Idempotent("sample-create-order")` —
same `Idempotency-Key` header within the configured TTL replays the cached
response instead of re-executing.

Headers:
```
Content-Type: application/json
Authorization: Bearer <accessToken>
Idempotency-Key: 7e2d1c0a-…              (recommended)
```

Request:
```json
{ "product": "widget", "quantity": 3 }
```

Response `200 OK`:
```json
{
  "status": "success",
  "data": {
    "orderId":   "f1b3e9c2-…",
    "createdBy": "alice",
    "product":   "widget",
    "quantity":  3
  }
}
```

Errors:
| Status | code                | When                                     |
|--------|---------------------|------------------------------------------|
| 400    | `VALIDATION_FAILED` | blank `product` or non-numeric `quantity`|
| 401    | `UNAUTHORIZED`      | gateway rejected the JWT                 |

---

## 4. `api-gateway`

Routing only — paths under `/api/**` are forwarded to discovered services.
The gateway exposes a single fallback endpoint of its own.

| # | Method | Path             | Auth | Description                                |
|---|--------|------------------|------|--------------------------------------------|
| 1 | GET    | `/fallback`      | none | RFC-7807 `503` body when downstream fails  |
| 2 | GET    | `/actuator/**`   | none | Spring Boot Actuator (subset)              |

### 4.1 `GET /fallback`

Returned automatically by the resilience4j circuit-breaker filter when a
route's downstream is unreachable.

Response `503 Service Unavailable`, `Content-Type: application/problem+json`:
```json
{
  "type":   "https://errors.enterprise.com/ERR-3003",
  "title":  "INTEGRATION_UNAVAILABLE",
  "status": 503,
  "detail": "The downstream service is temporarily unavailable.",
  "timestamp": "2026-04-27T10:00:00Z"
}
```

### 4.2 Routing table (from `config-server/.../api-gateway.yml`)

| Route id                  | Predicate                                | Target                | Filters         |
|---------------------------|------------------------------------------|-----------------------|-----------------|
| `auth-service`            | `Path=/api/auth/**`                      | `lb://auth-service`   | `StripPrefix=1` |
| `sample-service`          | `Path=/api/v1/sample/**`                 | `lb://sample-service` | —               |
| `sample-service-graphql`  | `Path=/graphql,/graphiql,/graphql/**`    | `lb://sample-service` | —               |

**Public paths** (skip `JwtAuthenticationGatewayFilter`):

```
/api/auth/login
/api/auth/refresh
/api/auth/register
/actuator/health
/actuator/info
```

Everything else under `/api/**` requires
`Authorization: Bearer <accessToken>`. Validation failures emit
`401 Unauthorized` with `X-Auth-Error: <reason>`.

Successful validation injects two headers into the proxied request:

```
X-Auth-Subject: <jwt.sub>
X-Auth-Roles:   <jwt.roles>
```

---

## 4a. GraphQL

`sample-service` additionally exposes a GraphQL endpoint that demonstrates
the platform's GraphQL conventions. Wiring lives in `common-lib` so any
service can opt in by adding `spring-boot-starter-graphql` and a schema
file under `classpath:/graphql/*.graphqls`.

| Endpoint               | Auth (gateway-injected) | Purpose                                  |
|------------------------|-------------------------|------------------------------------------|
| `POST /graphql`        | bearer (production)     | All queries / mutations                  |
| `GET  /graphiql`       | none (dev)              | Interactive UI; **disable in prod**      |
| `POST /graphql` (WS)   | bearer (production)     | Subscriptions (when starter-websocket on)|

### Configuration (via Config Server / `application.yml`)

```yaml
enterprise:
  common:
    graphql:
      enabled: true
      max-query-depth: 12          # reject deeper queries
      max-query-complexity: 150    # each field counts as 1 by default
      introspection:
        enabled: false             # turn off in production
      scalars:
        enabled: true              # registers DateTime, UUID, JSON, …
      error-mapping:
        enabled: true              # map ErrorCode → extensions.errorCode
        expose-message: true       # set false to redact internal messages

spring.graphql:
  schema:
    locations: classpath:/graphql/
  graphiql:
    enabled: ${GRAPHIQL_ENABLED:true}
    path: /graphiql
  path: /graphql
```

### Custom scalars (auto-registered)

`DateTime`, `Date`, `Time`, `UUID`, `JSON`, `Long`, `BigDecimal`, `Url`,
`Object` — provided by `graphql-java-extended-scalars`.

### `QueryResponse<T>` envelope

Every resolver should return `QueryResponse<T>` so clients see the same
shape they get from REST `ApiResponse<T>`. Schema convention:

```graphql
type WhoAmIEnvelope {
    status: String!
    data: WhoAmI
    message: String
    correlationId: String
    timestamp: DateTime!
    warnings: [String!]
    extensions: JSON
}
```

Java:

```java
@QueryMapping
public QueryResponse<WhoAmI> whoAmI(@Argument String subject,
                                    @Argument String roles) {
    return QueryResponse.ok(new WhoAmI(subject, roles, MDC.get("correlationId")));
}
```

### Errors

Failures emit standard GraphQL errors. The platform's
`GraphQlExceptionResolver` populates `extensions` with the same
`ErrorCode` catalog used by REST:

```json
{
  "errors": [
    {
      "message": "Invalid username or password.",
      "path": ["login"],
      "extensions": {
        "errorCode":      "ERR-1003",
        "classification": "INVALID_CREDENTIALS",
        "correlationId":  "abc-123",
        "timestamp":      "2026-04-29T12:00:00Z"
      }
    }
  ],
  "data": { "login": null }
}
```

Mapping (Java exception → GraphQL `ErrorType` + `ErrorCode`):

| Exception                                        | `ErrorType`      | `ErrorCode`               |
|--------------------------------------------------|------------------|---------------------------|
| `ConstraintViolationException` / `BusinessException(VALIDATION_FAILED)` | `BAD_REQUEST` | `VALIDATION_FAILED` |
| `BadCredentialsException`                        | `UNAUTHORIZED`   | `INVALID_CREDENTIALS`     |
| `LockedException`                                | `UNAUTHORIZED`   | `ACCOUNT_LOCKED`          |
| `DisabledException`                              | `FORBIDDEN`      | `ACCOUNT_DISABLED`        |
| `AccessDeniedException`                          | `FORBIDDEN`      | `ACCESS_DENIED`           |
| `ExpiredJwtException`                            | `UNAUTHORIZED`   | `TOKEN_EXPIRED`           |
| `JwtException` (any other)                       | `UNAUTHORIZED`   | `TOKEN_INVALID`           |
| `DuplicateKeyException`                          | `BAD_REQUEST`    | `DUPLICATE_RESOURCE`      |
| `DataIntegrityViolationException`                | `BAD_REQUEST`    | `DATA_INTEGRITY_VIOLATION`|
| `OptimisticLockingFailureException`              | `BAD_REQUEST`    | `OPTIMISTIC_LOCK`         |
| `EmptyResultDataAccessException`                 | `NOT_FOUND`      | `NOT_FOUND`               |
| `IllegalArgumentException`                       | `BAD_REQUEST`    | `BAD_REQUEST`             |
| `UnsupportedOperationException`                  | `INTERNAL_ERROR` | `NOT_IMPLEMENTED`         |
| any other `Throwable`                            | `INTERNAL_ERROR` | `INTERNAL_ERROR`          |

### Hardening

* `MaxQueryDepthInstrumentation` (default 15) — rejects deeply nested queries.
* `MaxQueryComplexityInstrumentation` (default 200) — rejects expensive queries.
* `introspection.enabled=false` in prod hides the schema from the client.
* `error-mapping.expose-message=false` makes the resolver use the
  classification code instead of the localized message — useful when
  internal messages might leak business context.

### Sample queries

```bash
GW=http://localhost:8080

# whoAmI
curl -X POST $GW/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS" \
  -d '{"query":"{ whoAmI(subject:\"alice\",roles:\"[ROLE_USER]\") { status data { subject roles correlationId } } }"}'

# createOrder mutation
curl -X POST $GW/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"query":"mutation($i:CreateOrderInput!){ createOrder(input:$i,subject:\"alice\"){ data { orderId product quantity } } }","variables":{"i":{"product":"widget","quantity":3}}}'
```

---

## 5. Platform endpoints (every service)

All Spring Boot services expose a uniform **Actuator** surface. Visibility
is governed by `management.endpoints.web.exposure.include` (currently
`health,info,metrics,prometheus,env,loggers,refresh`).

| Method | Path                              | Notes                                |
|--------|-----------------------------------|--------------------------------------|
| GET    | `/actuator/health`                | Aggregate status                     |
| GET    | `/actuator/health/liveness`       | Probe – is the process alive?        |
| GET    | `/actuator/health/readiness`      | Probe – ready to receive traffic?    |
| GET    | `/actuator/info`                  | git/build info (via `git.properties`)|
| GET    | `/actuator/metrics`               | Micrometer registry                  |
| GET    | `/actuator/prometheus`            | Prometheus scrape format             |
| GET    | `/actuator/env`                   | Resolved configuration               |
| GET    | `/actuator/loggers`               | Read levels                          |
| POST   | `/actuator/loggers/{name}`        | Change a logger's level              |
| POST   | `/actuator/refresh`               | Re-bind `@RefreshScope` beans        |
| GET    | `/v3/api-docs`                    | OpenAPI 3 JSON (where Springdoc on)  |
| GET    | `/swagger-ui.html`                | Swagger UI                           |

`config-server` and `service-registry` additionally require **HTTP Basic**
on every endpoint (default `admin:admin` / `eureka:eureka` — change in prod).

Eureka exposes:

| Method | Path                              | Description                       |
|--------|-----------------------------------|-----------------------------------|
| GET    | `/eureka/apps`                    | Registry XML                      |
| GET    | `/eureka/apps/{appName}`          | Single app                        |
| GET    | `/`                               | Eureka dashboard                  |

Config Server exposes:

| Method | Path                                       | Description               |
|--------|--------------------------------------------|---------------------------|
| GET    | `/{application}/{profile}[/{label}]`       | Composite config          |
| GET    | `/{application}-{profile}.yml`             | YAML view                 |
| GET    | `/{application}-{profile}.properties`      | properties view           |

---

## 6. Common envelopes

### `ApiResponse<T>` — success envelope

```json
{
  "status":        "success",
  "data":          { /* generic T */ },
  "message":       "optional human-readable",
  "correlationId": "<MDC[correlationId]>",
  "timestamp":     "2026-04-27T10:00:00Z"
}
```

### `ProblemDetail` — error envelope (RFC 7807)

`Content-Type: application/problem+json`

```json
{
  "type":     "https://errors.enterprise.com/<code>",
  "title":    "<short, code-shaped>",
  "status":   <http-status>,
  "detail":   "<human readable>",
  "instance": "/api/auth/login",
  "code":     "INVALID_CREDENTIALS",
  "errors":   [
    { "field": "password", "message": "must not be blank" }
  ],
  "correlationId": "abc-123",
  "timestamp": "2026-04-27T10:00:00Z"
}
```

### Error code catalog (`ErrorCode` enum — complete)

#### Generic
| Code                       | Code id  | HTTP | Triggers                                             |
|----------------------------|----------|------|------------------------------------------------------|
| `INTERNAL_ERROR`           | ERR-0001 | 500  | Unhandled / catch-all                                |
| `VALIDATION_FAILED`        | ERR-0002 | 400  | `@Valid`, `@Validated`, `MethodArgumentNotValid…`    |
| `BAD_REQUEST`              | ERR-0003 | 400  | Generic bad input, `IllegalArgumentException`        |
| `NOT_FOUND`                | ERR-0004 | 404  | `NoHandlerFoundException`, `EmptyResultDataAccess…`  |
| `CONFLICT`                 | ERR-0005 | 409  | Generic conflict                                     |
| `METHOD_NOT_ALLOWED`       | ERR-0006 | 405  | `HttpRequestMethodNotSupportedException`             |
| `UNSUPPORTED_MEDIA_TYPE`   | ERR-0007 | 415  | `HttpMediaTypeNotSupportedException`                 |
| `NOT_ACCEPTABLE`           | ERR-0008 | 406  | `HttpMediaTypeNotAcceptableException`                |
| `PAYLOAD_TOO_LARGE`        | ERR-0009 | 413  | `MaxUploadSizeExceededException`                     |
| `MISSING_PARAMETER`        | ERR-0010 | 400  | `MissingServletRequestParameterException`            |
| `MISSING_HEADER`           | ERR-0011 | 400  | `MissingRequestHeaderException`                      |
| `MALFORMED_JSON`           | ERR-0012 | 400  | `HttpMessageNotReadableException`                    |
| `NOT_IMPLEMENTED`          | ERR-0013 | 501  | `UnsupportedOperationException`                      |
| `REQUEST_TIMEOUT`          | ERR-0014 | 408  | Request side timeout                                 |

#### Security
| Code                  | Code id  | HTTP | Triggers                                  |
|-----------------------|----------|------|-------------------------------------------|
| `UNAUTHORIZED`        | ERR-1001 | 401  | `AuthenticationException` (any)           |
| `FORBIDDEN`           | ERR-1002 | 403  | Generic forbidden                         |
| `INVALID_CREDENTIALS` | ERR-1003 | 401  | `BadCredentialsException`                 |
| `TOKEN_EXPIRED`       | ERR-1004 | 401  | `ExpiredJwtException`                     |
| `TOKEN_INVALID`       | ERR-1005 | 401  | `Malformed/Unsupported/SignatureException`|
| `ACCESS_DENIED`       | ERR-1006 | 403  | `AccessDeniedException`                   |
| `ACCOUNT_LOCKED`      | ERR-1007 | 423  | `LockedException`, lockout policy         |
| `ACCOUNT_DISABLED`    | ERR-1008 | 403  | `DisabledException`                       |
| `ACCOUNT_EXPIRED`     | ERR-1009 | 403  | `AccountExpiredException`                 |
| `CREDENTIALS_EXPIRED` | ERR-1010 | 403  | `CredentialsExpiredException`             |

#### Business
| Code                       | Code id  | HTTP | Triggers                                  |
|----------------------------|----------|------|-------------------------------------------|
| `BUSINESS_RULE_VIOLATION`  | ERR-2001 | 422  | Domain invariant failed                   |
| `RESOURCE_LOCKED`          | ERR-2002 | 423  | `CannotAcquireLock…` / pessimistic lock   |
| `RESOURCE_EXPIRED`         | ERR-2003 | 410  | Resource gone                             |
| `DUPLICATE_RESOURCE`       | ERR-2004 | 409  | `DuplicateKeyException`, unique key       |
| `OPTIMISTIC_LOCK`          | ERR-2005 | 409  | `OptimisticLockingFailureException`       |
| `PRECONDITION_FAILED`      | ERR-2006 | 412  | If-Match / If-Unmodified-Since failed     |

#### Persistence / database
| Code                       | Code id  | HTTP | Triggers                                  |
|----------------------------|----------|------|-------------------------------------------|
| `DATABASE_ERROR`           | ERR-5001 | 500  | `DataAccessException` / `SQLException`    |
| `DATABASE_UNAVAILABLE`     | ERR-5002 | 503  | `DataAccessResourceFailureException`      |
| `DATABASE_TIMEOUT`         | ERR-5003 | 504  | `QueryTimeoutException`                   |
| `DATA_INTEGRITY_VIOLATION` | ERR-5004 | 409  | `DataIntegrityViolationException`         |

#### Integration / resilience
| Code                      | Code id  | HTTP | Triggers                                  |
|---------------------------|----------|------|-------------------------------------------|
| `INTEGRATION_ERROR`       | ERR-3001 | 502  | Upstream returned error                   |
| `INTEGRATION_TIMEOUT`     | ERR-3002 | 504  | Upstream / `TimeoutException`             |
| `INTEGRATION_UNAVAILABLE` | ERR-3003 | 503  | Upstream unreachable                      |
| `CIRCUIT_OPEN`            | ERR-3004 | 503  | `CallNotPermittedException`               |
| `BULKHEAD_FULL`           | ERR-3005 | 503  | `BulkheadFullException`                   |

#### Rate limiting / idempotency
| Code                  | Code id  | HTTP | Triggers                                       |
|-----------------------|----------|------|------------------------------------------------|
| `TOO_MANY_REQUESTS`   | ERR-4001 | 429  | RateLimiter / `RequestNotPermitted`            |
| `IDEMPOTENCY_REPLAY`  | ERR-4002 | 409  | Same `Idempotency-Key` w/ different body       |

> Every `ErrorCode` is also a key into `i18n/messages*.properties`, so the
> human-readable text follows the request `Accept-Language` header.

---

## 7. Common headers

### Request

| Header              | When                | Meaning                                       |
|---------------------|---------------------|-----------------------------------------------|
| `Authorization`     | protected endpoints | `Bearer <accessToken>` (HS256 JWT)            |
| `X-Correlation-ID`  | optional            | propagated end-to-end; generated if absent    |
| `Idempotency-Key`   | `@Idempotent` POSTs | UUID; replay-safe within TTL                  |
| `Accept-Language`   | optional            | drives i18n message resolution                |
| `X-Gateway-Secret`  | downstream only     | shared secret for `TrustedGatewayHeaderFilter`|

### Response

| Header             | Meaning                                                    |
|--------------------|------------------------------------------------------------|
| `X-Correlation-ID` | echo of the request id (or generated)                      |
| `X-Request-ID`     | per-hop UUID (when `generate-request-id=true`)             |
| `X-Auth-Error`     | gateway sets on `401` to describe why JWT was rejected     |
| `X-Auth-Subject`   | gateway → downstream: validated `sub` claim                |
| `X-Auth-Roles`     | gateway → downstream: validated `roles` claim              |
| `Content-Security-Policy` | `auth-service` only: `default-src 'none'; …`        |
| `Referrer-Policy`  | `strict-origin-when-cross-origin` (auth-service)           |

---

## 8. Quick reference (cheat sheet)

```bash
GW=http://localhost:8080

# Login → save tokens
RESP=$(curl -s -X POST $GW/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}')
ACCESS=$(echo "$RESP"  | jq -r '.data.accessToken')
REFRESH=$(echo "$RESP" | jq -r '.data.refreshToken')

# Call sample-service
curl $GW/api/v1/sample/whoami -H "Authorization: Bearer $ACCESS"

# Idempotent order
curl -X POST $GW/api/v1/sample/orders \
  -H "Authorization: Bearer $ACCESS" \
  -H "Idempotency-Key: $(uuidgen)" \
  -H "Content-Type: application/json" \
  -d '{"product":"widget","quantity":3}'

# Rotate refresh token
curl -X POST $GW/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"

# Logout (single token)
curl -X POST $GW/api/auth/logout \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"

# Logout everywhere
curl -X POST $GW/api/auth/logout/all \
  -H "Authorization: Bearer $ACCESS"
```

---

## 9. OpenAPI (machine-readable)

Each service that includes `springdoc-openapi-starter-webmvc-ui` exposes
its full schema:

| Service          | OpenAPI JSON                                  | Swagger UI                              |
|------------------|-----------------------------------------------|-----------------------------------------|
| `auth-service`   | `http://localhost:9000/v3/api-docs`           | `http://localhost:9000/swagger-ui.html` |
| `sample-service` | `http://localhost:9100/v3/api-docs`           | `http://localhost:9100/swagger-ui.html` |

To export the bundles in CI add the `springdoc-openapi-maven-plugin`
`generate` execution; the resulting `openapi.json` can be fed to
`openapi-generator-cli` for SDK generation.

---

## 10. What is intentionally **not** exposed

| Capability             | Reason                                                |
|------------------------|-------------------------------------------------------|
| User self-registration | Out of scope for the skeleton; add via `/register`    |
| Password reset / email | Plug your provider (SES, SendGrid) into a new module  |
| MFA / TOTP             | Extension point — see ADR-0003 for the auth model     |
| Messaging / Kafka      | Excluded by request from the skeleton                 |
| Multi-tenancy          | Tenant header is propagated but no tenant resolver    |

These are documented so adopters know exactly where to extend.
