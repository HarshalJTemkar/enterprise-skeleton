# API Gateway

Enterprise-grade API Gateway built with Spring Cloud Gateway (reactive) for the enterprise-skeleton microservices platform.

## Overview

The API Gateway serves as the single entry point for all client requests, providing:

- **Intelligent Routing** - Load-balanced routing to microservices via Eureka service discovery
- **JWT Authentication** - Centralized token validation with header propagation to downstream services  
- **Rate Limiting** - Redis-backed distributed throttling with tiered policies
- **API Versioning** - Multi-strategy version handling (path, header, media-type)
- **Circuit Breaking** - Resilience4j integration with fallback handling
- **Observability** - Metrics, tracing, and structured logging

## Features

### 🔐 Security
- JWT token validation via `JwtAuthenticationGatewayFilter`
- Configurable public/private endpoints
- Subject and roles propagated to downstream via `X-Auth-Subject` and `X-Auth-Roles` headers
- CORS configuration with credential support

### 🚦 Rate Limiting & Throttling
- **Distributed** - Redis-backed rate limiting across gateway instances
- **Tiered Policies** - Different limits for different endpoint categories:
  - Critical (login): 5 req/sec
  - Sensitive (auth): 30 req/sec
  - Standard (APIs): 100 req/sec
  - GraphQL: 20 req/sec
  - Public: 50 req/sec
- **Flexible Keys** - IP-based or user-based (principal) rate limiting

### 🔢 API Versioning
- **Path-based**: `/api/v1/sample/**` vs `/api/v2/sample/**`
- **Header-based**: `X-API-Version: v1` or `X-API-Version: 2`
- **Media-type**: `Accept: application/vnd.enterprise.v1+json`
- **Validation** - Optional version validation against supported versions
- **Normalization** - Handles `v1`, `1`, `1.0` formats consistently

### 🛡️ Resilience
- **Circuit Breaker** - Per-route circuit breakers with `/fallback` endpoint
- **Retry Logic** - Automatic retries on transient failures (BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT)
- **Timeouts** - Configurable connect and response timeouts
- **Graceful Degradation** - Structured error responses via `FallbackController`

### 📊 Observability
- **Actuator Endpoints** - Health, metrics, Prometheus, gateway routes
- **Micrometer Metrics** - Request rates, latencies, circuit breaker stats
- **Distributed Tracing** - OpenTelemetry support (optional)
- **Structured Logging** - JSON logging with correlation IDs

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+
- Redis (for rate limiting)
- Config Server (running on port 8888)
- Service Registry/Eureka (running on port 8761)

### Run Locally

```bash
# 1. Start dependencies (Redis, Config Server, Eureka)
docker-compose up -d redis config-server service-registry

# 2. Build the gateway
mvn clean package -DskipTests

# 3. Run the gateway
java -jar target/api-gateway.jar

# Or with Maven
mvn spring-boot:run
```

The gateway will start on **http://localhost:8080**

### Configuration

The gateway uses **Spring Cloud Config** for centralized configuration. Settings are loaded from `config-server/src/main/resources/config/api-gateway.yml`.

Key environment variables:

```bash
# Config Server
CONFIG_SERVER_URI=http://localhost:8888
CONFIG_SERVER_USER=admin
CONFIG_SERVER_PASSWORD=admin

# Eureka
EUREKA_SERVICE_URL=http://eureka:eureka@localhost:8761/eureka/

# Redis (for rate limiting)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT
JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long

# Profiles
SPRING_PROFILES_ACTIVE=dev
```

## Routes

### Configured Routes

| Route ID | Path | Service | Features |
|----------|------|---------|----------|
| `auth-login` | `/api/auth/login` | auth-service | Critical throttling (5 req/sec), Circuit breaker |
| `auth-service` | `/api/auth/**` | auth-service | Sensitive throttling (30 req/sec), Circuit breaker |
| `sample-service-v1` | `/api/v1/sample/**` | sample-service | Version header validation, Circuit breaker |
| `sample-service-v2` | `/api/v2/sample/**` | sample-service | Version header validation, Circuit breaker |
| `sample-service-default` | `/api/v1/sample/**` | sample-service | Default v1 fallback, Circuit breaker |
| `sample-service-graphql` | `/graphql/**` | sample-service | GraphQL throttling (20 req/sec), Circuit breaker |

### Public Endpoints (No JWT Required)

- `/api/auth/login`
- `/api/auth/refresh`
- `/api/auth/register`
- `/actuator/health/**`
- `/actuator/prometheus`
- `/fallback/**`
- `/graphiql/**`
- `/v3/api-docs/**`
- `/swagger-ui/**`

## Usage Examples

### Authentication

```bash
# Login (public endpoint)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# Response: {"accessToken":"eyJhbGci...", "refreshToken":"..."}

# Authenticated request
curl http://localhost:8080/api/v1/sample/123 \
  -H "Authorization: Bearer eyJhbGci..."
```

### API Versioning

```bash
# Path-based versioning
curl http://localhost:8080/api/v1/sample/123
curl http://localhost:8080/api/v2/sample/123

# Header-based versioning
curl http://localhost:8080/api/v1/sample/123 \
  -H "X-API-Version: v2"

# Media-type versioning
curl http://localhost:8080/api/v1/sample/123 \
  -H "Accept: application/vnd.enterprise.v2+json"
```

### Rate Limiting

```bash
# Test rate limiting (will 429 after burst capacity)
for i in {1..15}; do
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test"}'
done
```

### Actuator Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Gateway routes
curl http://localhost:8080/actuator/gateway/routes | jq

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

## Architecture

```
┌─────────────┐
│   Clients   │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│              API Gateway (Port 8080)             │
│                                                  │
│  ┌────────────────────────────────────────────┐ │
│  │  Global Filters                            │ │
│  │  • Retry                                   │ │
│  │  • Circuit Breaker                         │ │
│  │  • Rate Limiter (Redis)                    │ │
│  │  • Request Size Limiter                    │ │
│  │  • CORS                                    │ │
│  └────────────────────────────────────────────┘ │
│                                                  │
│  ┌────────────────────────────────────────────┐ │
│  │  Custom Filters                            │ │
│  │  • JwtAuthenticationGatewayFilter          │ │
│  │  • ApiVersionGatewayFilter                 │ │
│  └────────────────────────────────────────────┘ │
│                                                  │
│  ┌────────────────────────────────────────────┐ │
│  │  Route Predicates                          │ │
│  │  • Path matching                           │ │
│  │  • Header matching (versions)              │ │
│  │  • Method matching                         │ │
│  └────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────┐
│           Service Discovery (Eureka)             │
└──────────────────────────────────────────────────┘
       │
       ├─────────────┬─────────────┬───────────────┐
       ▼             ▼             ▼               ▼
  ┌─────────┐  ┌──────────┐  ┌──────────┐   ┌─────────┐
  │  Auth   │  │  Sample  │  │  Other   │   │  Other  │
  │ Service │  │ Service  │  │ Service  │   │ Service │
  └─────────┘  └──────────┘  └──────────┘   └─────────┘
```

## Configuration Details

### Rate Limiting Tiers

Defined in `api-gateway.yml`:

```yaml
gateway:
  throttling:
    critical:           # Login, password reset
      replenish-rate: 5
      burst-capacity: 10
    sensitive:          # Auth operations
      replenish-rate: 30
      burst-capacity: 50
    standard:           # General APIs
      replenish-rate: 100
      burst-capacity: 200
    graphql:            # GraphQL queries
      replenish-rate: 20
      burst-capacity: 40
    public-api:         # Public endpoints
      replenish-rate: 50
      burst-capacity: 100
```

### Circuit Breaker

Default circuit breaker configuration (Resilience4j):

```yaml
resilience4j:
  circuitbreaker:
    instances:
      defaultCB:
        sliding-window-size: 20
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 5
```

### HTTP Client

Global HTTP client settings:

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 2000ms
        response-timeout: 5s
        pool:
          max-connections: 500
          acquire-timeout: 45s
        compression: true
```

## Development

### Running Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# With coverage
mvn clean verify jacoco:report
```

### Code Quality

```bash
# Format code
mvn spotless:apply

# Check style
mvn checkstyle:check

# Security scan
mvn dependency-check:check
```

### Building

```bash
# Build JAR
mvn clean package

# Build Docker image
docker build -t api-gateway:latest .

# Build with layered JAR
mvn clean package spring-boot:build-image
```

## Docker

### Dockerfile

Multi-stage build with layered JARs for optimal image size:

```dockerfile
FROM eclipse-temurin:21-jre-noble
# ... see Dockerfile for full content
```

### Run with Docker

```bash
# Build image
docker build -t api-gateway:latest .

# Run container
docker run -p 8080:8080 \
  -e CONFIG_SERVER_URI=http://config-server:8888 \
  -e EUREKA_SERVICE_URL=http://eureka:eureka@service-registry:8761/eureka/ \
  -e REDIS_HOST=redis \
  -e SPRING_PROFILES_ACTIVE=prod \
  api-gateway:latest
```

### Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f api-gateway

# Stop services
docker-compose down
```

## Kubernetes

Deploy to Kubernetes:

```bash
# Apply configurations
kubectl apply -f kubernetes/api-gateway-deployment.yml
kubectl apply -f kubernetes/api-gateway-service.yml

# Check status
kubectl get pods -l app=api-gateway
kubectl logs -f deployment/api-gateway
```

## Monitoring

### Metrics

Key metrics exposed via `/actuator/prometheus`:

- `spring_cloud_gateway_requests_seconds_count` - Request count per route
- `spring_cloud_gateway_requests_seconds_sum` - Request duration per route
- `http_server_requests_seconds_count` - Standard HTTP metrics
- `resilience4j_circuitbreaker_calls_seconds_count` - Circuit breaker calls

### Prometheus Queries

```promql
# Request rate by route
rate(spring_cloud_gateway_requests_seconds_count{route_id="auth-login"}[5m])

# 95th percentile latency
histogram_quantile(0.95, rate(spring_cloud_gateway_requests_seconds_bucket[5m]))

# Rate limit rejections (429s)
sum(rate(http_server_requests_seconds_count{status="429"}[5m])) by (uri)

# Circuit breaker state
resilience4j_circuitbreaker_state{name="authServiceCB"}
```

### Health Checks

```bash
# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness
```

## Troubleshooting

### Gateway not routing requests

1. Check service registration:
   ```bash
   curl http://localhost:8761/eureka/apps
   ```

2. Verify gateway routes:
   ```bash
   curl http://localhost:8080/actuator/gateway/routes | jq
   ```

3. Check gateway logs:
   ```bash
   kubectl logs -f deployment/api-gateway
   ```

### Rate limiting not working

1. Verify Redis connection:
   ```bash
   redis-cli -h localhost -p 6379 ping
   ```

2. Check rate limit keys:
   ```bash
   redis-cli KEYS "request_rate_limiter*"
   ```

3. Enable debug logging:
   ```yaml
   logging:
     level:
       org.springframework.cloud.gateway: DEBUG
   ```

### JWT validation failing

1. Check JWT secret matches across services
2. Verify token hasn't expired
3. Check public paths configuration
4. Review filter order (JWT filter runs at order -100)

## Security Considerations

- **JWT Secret**: Use strong secrets (32+ characters) and rotate regularly
- **HTTPS**: Always use HTTPS in production (configure SSL/TLS)
- **Rate Limiting**: Adjust limits based on traffic patterns
- **CORS**: Tighten CORS policies for production
- **Public Endpoints**: Minimize publicly accessible endpoints
- **Headers**: Remove sensitive headers in responses
- **Timeouts**: Set appropriate timeouts to prevent resource exhaustion

## Performance Tuning

### JVM Options

```bash
JAVA_OPTS="-XX:+UseZGC \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+ExitOnOutOfMemoryError \
  -Xlog:gc:file=gc.log \
  -Djava.security.egd=file:/dev/./urandom"
```

### Virtual Threads

Enable virtual threads for improved concurrency:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### Connection Pool

Adjust HTTP client pool:

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          max-connections: 1000
          acquire-timeout: 45s
```

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) in the root directory.

## License

Apache License 2.0 - See [LICENSE](../LICENSE)

## Documentation

- [Throttling & Versioning Guide](./THROTTLING_AND_VERSIONING.md)
- [Implementation Summary](./IMPLEMENTATION_SUMMARY.md)
- [Enterprise Skeleton README](../README.md)

## Support

- GitHub Issues: https://github.com/enterprise/enterprise-skeleton/issues
- Documentation: https://github.com/enterprise/enterprise-skeleton/wiki
