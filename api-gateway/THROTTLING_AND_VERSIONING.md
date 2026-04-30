# API Gateway Throttling & Versioning

This document describes the enhanced throttling and API versioning features added to the API Gateway.

## Throttling / Rate Limiting

### Overview
The API Gateway implements **Redis-backed distributed rate limiting** with tiered throttling policies for different endpoint categories.

### Throttling Tiers

| Tier | Replenish Rate | Burst Capacity | Use Case |
|------|----------------|----------------|----------|
| **Critical** | 5 req/sec | 10 | Login, password reset, OTP verification |
| **Sensitive** | 30 req/sec | 50 | Token refresh, user registration, profile updates |
| **Standard** | 100 req/sec | 200 | General API endpoints (default) |
| **GraphQL** | 20 req/sec | 40 | GraphQL queries (can be expensive) |
| **Public API** | 50 req/sec | 100 | Public/unauthenticated endpoints |

### Configuration

Defined in `api-gateway.yml`:

```yaml
gateway:
  throttling:
    critical:
      replenish-rate: 5
      burst-capacity: 10
    sensitive:
      replenish-rate: 30
      burst-capacity: 50
    # ... etc
```

### Per-Route Throttling Examples

#### Login Endpoint (Critical)
```yaml
- id: auth-login
  uri: lb://auth-service
  predicates:
    - Path=/api/auth/login
  filters:
    - name: RequestRateLimiter
      args:
        redis-rate-limiter.replenishRate: 5
        redis-rate-limiter.burstCapacity: 10
        key-resolver: "#{@ipKeyResolver}"
```

#### GraphQL Endpoint (Expensive Queries)
```yaml
- id: sample-service-graphql
  predicates:
    - Path=/graphql,/graphql/**
  filters:
    - name: RequestRateLimiter
      args:
        redis-rate-limiter.replenishRate: 20
        redis-rate-limiter.burstCapacity: 40
        key-resolver: "#{@ipKeyResolver}"
```

### Rate Limiting Strategies

Two `KeyResolver` beans are available in `RateLimiterConfig`:

1. **`ipKeyResolver`** (default) - Rate limit by client IP address
   - Handles `X-Forwarded-For` header for proxied requests
   - Best for public-facing endpoints

2. **`principalKeyResolver`** - Rate limit by authenticated user
   - Uses `X-Auth-Subject` header (populated by JWT filter)
   - Best for authenticated API endpoints

To use principal-based rate limiting:
```yaml
key-resolver: "#{@principalKeyResolver}"
```

### Redis Configuration

Required for distributed rate limiting:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2s
```

---

## API Versioning

### Overview
The API Gateway supports **multi-faceted versioning** strategies:

1. **Path-based versioning** - `/api/v1/sample/**` vs `/api/v2/sample/**`
2. **Header-based versioning** - `X-API-Version: v1` or `X-API-Version: 2`
3. **Media type versioning** - `Accept: application/vnd.enterprise.v1+json`

### Version Routing

Routes are configured to handle different versions:

```yaml
# Version 1 (with header)
- id: sample-service-v1
  uri: lb://sample-service
  predicates:
    - Path=/api/v1/sample/**
    - Header=X-API-Version, v1|1|1\\..*
  filters:
    - AddRequestHeader=X-Resolved-API-Version, v1

# Version 2 (future)
- id: sample-service-v2
  uri: lb://sample-service
  predicates:
    - Path=/api/v2/sample/**
    - Header=X-API-Version, v2|2|2\\..*
  filters:
    - AddRequestHeader=X-Resolved-API-Version, v2

# Default to v1 (no version header)
- id: sample-service-default
  uri: lb://sample-service
  predicates:
    - Path=/api/v1/sample/**
  filters:
    - AddRequestHeader=X-Resolved-API-Version, v1
```

### ApiVersionGatewayFilter

A custom filter (`ApiVersionGatewayFilter`) provides advanced version handling:

**Features:**
- Extracts version from multiple sources (header, media type)
- Validates version against supported versions
- Normalizes version format (`v1`, `1`, `1.0` → `v1`)
- Forwards resolved version to downstream via `X-Resolved-API-Version` header

**Usage:**
```yaml
filters:
  - name: ApiVersion
    args:
      defaultVersion: v1
      supportedVersions:
        - v1
        - v2
      required: false
      enableVendorMediaType: true
      vendorPrefix: enterprise
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `defaultVersion` | String | `v1` | Version to use if not specified |
| `supportedVersions` | List | `[v1]` | List of supported versions |
| `required` | Boolean | `false` | Whether version is mandatory |
| `enableVendorMediaType` | Boolean | `false` | Parse version from Accept header |
| `vendorPrefix` | String | `enterprise` | Vendor prefix for media types |

### Version Header Behavior

1. **Client sends version:**
   ```
   GET /api/v1/sample/123
   X-API-Version: v1
   ```
   → Gateway adds `X-Resolved-API-Version: v1`

2. **Client omits version:**
   ```
   GET /api/v1/sample/123
   ```
   → Gateway adds `X-Resolved-API-Version: v1` (default)

3. **Client uses media type:**
   ```
   GET /api/v1/sample/123
   Accept: application/vnd.enterprise.v2+json
   ```
   → Gateway extracts `v2` and adds `X-Resolved-API-Version: v2`

### Downstream Service Integration

Downstream services receive the resolved version via header:

```java
@GetMapping("/sample/{id}")
public ResponseEntity<Sample> getSample(
    @PathVariable String id,
    @RequestHeader(value = "X-Resolved-API-Version", defaultValue = "v1") String apiVersion
) {
    // Conditional logic based on API version
    if ("v2".equals(apiVersion)) {
        // Return v2 response format
    } else {
        // Return v1 response format
    }
}
```

---

## Default Filters

All routes inherit these default filters:

### 1. Retry Filter
```yaml
- name: Retry
  args:
    retries: 2
    statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE,GATEWAY_TIMEOUT
    methods: GET,HEAD,OPTIONS
    backoff:
      firstBackoff: 100ms
      maxBackoff: 500ms
      factor: 2
```

### 2. Default Rate Limiter
```yaml
- name: RequestRateLimiter
  args:
    redis-rate-limiter.replenishRate: 100
    redis-rate-limiter.burstCapacity: 200
    key-resolver: "#{@ipKeyResolver}"
```

### 3. Circuit Breaker
Each route includes a circuit breaker with fallback:

```yaml
- name: CircuitBreaker
  args:
    name: authServiceCB
    fallbackUri: forward:/fallback
```

---

## Testing

### Test Rate Limiting

```bash
# Hammer login endpoint - should get 429 after 10 requests
for i in {1..20}; do
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test"}'
done
```

### Test API Versioning

```bash
# Test with version header
curl http://localhost:8080/api/v1/sample/123 \
  -H "X-API-Version: v1"

# Test with media type
curl http://localhost:8080/api/v1/sample/123 \
  -H "Accept: application/vnd.enterprise.v1+json"

# Test unsupported version (should fail if filter is strict)
curl http://localhost:8080/api/v1/sample/123 \
  -H "X-API-Version: v99"
```

### View Gateway Routes

```bash
curl http://localhost:8080/actuator/gateway/routes | jq
```

---

## Monitoring

### Metrics

Rate limiting metrics are exposed via Micrometer:

- `spring.cloud.gateway.requests` - Tagged by route, status, outcome
- `http.server.requests` - Standard HTTP metrics

### Prometheus Queries

```promql
# Request rate by route
rate(spring_cloud_gateway_requests_seconds_count{route_id="auth-login"}[5m])

# Rate limit rejections (429 responses)
sum(rate(http_server_requests_seconds_count{status="429"}[5m])) by (uri)
```

---

## Best Practices

1. **Start Conservative**: Begin with stricter limits and gradually increase based on monitoring
2. **Different Keys**: Use IP-based limiting for public endpoints, principal-based for authenticated
3. **Burst Capacity**: Set burst capacity ≥ 2x replenish rate for handling traffic spikes
4. **Version Strategy**: Choose ONE versioning strategy per API (path OR header, not both)
5. **Deprecation**: Use version-specific rate limits to encourage migration (tighter limits on old versions)

---

## Configuration Reference

### Complete Route Example

```yaml
- id: auth-login
  uri: lb://auth-service
  order: 1
  predicates:
    - Path=/api/auth/login
    - Method=POST
  filters:
    - StripPrefix=1
    - name: RequestRateLimiter
      args:
        redis-rate-limiter.replenishRate: 5
        redis-rate-limiter.burstCapacity: 10
        redis-rate-limiter.requestedTokens: 1
        key-resolver: "#{@ipKeyResolver}"
    - name: CircuitBreaker
      args:
        name: authLoginCB
        fallbackUri: forward:/fallback
        statusCodes: 500, 502, 503, 504
    - name: ApiVersion
      args:
        defaultVersion: v1
        supportedVersions: [v1, v2]
        required: false
```

---

## Troubleshooting

### Rate Limiting Not Working

1. **Check Redis connection:**
   ```bash
   redis-cli -h localhost -p 6379 ping
   ```

2. **Verify KeyResolver bean:**
   ```bash
   curl http://localhost:8080/actuator/beans | jq '.contexts[].beans | keys | .[] | select(contains("KeyResolver"))'
   ```

3. **Check rate limit keys in Redis:**
   ```bash
   redis-cli -h localhost -p 6379 KEYS "request_rate_limiter*"
   ```

### Version Routing Issues

1. **Check route order** - More specific routes should have lower order values
2. **Test predicates** - Use `/actuator/gateway/routes` to see route definitions
3. **Check headers** - Ensure `X-Resolved-API-Version` is being set

### Circuit Breaker Not Triggering

1. **Check fallback endpoint** - Ensure `/fallback` is in JWT public paths
2. **Verify Resilience4j config** - Check `application.yml` for circuit breaker settings
3. **Monitor breaker state** - Use `/actuator/circuitbreakers` endpoint
