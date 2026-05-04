# Enterprise Skeleton - Service Startup Guide

## Issues Found and Resolution

### Problem Summary
The **ApiGatewayApplication** and **AuthServiceApplication** were not running due to:
1. **Code formatting violations** (Spotless plugin) preventing successful builds
2. **Missing infrastructure services** (Service Registry and Config Server) that these applications depend on

### Solution Applied
✅ **Fixed**: Applied Spotless code formatting to all files using `mvnw spotless:apply`
✅ **Fixed**: Successfully compiled both services

## Current Status

### Auth Service
- ✅ **Status**: RUNNING on port **9000**
- ⚠️ **Warning**: Cannot connect to Service Registry (Eureka) on port 8761
- ⚠️ **Warning**: Cannot connect to Config Server on port 8888
- **Note**: Service is functional despite warnings, but missing service discovery capabilities

### API Gateway
- ❌ **Status**: Not started yet
- **Dependencies**: Requires Service Registry (Eureka) to be running

## Microservices Architecture - Startup Order

This is a **Spring Cloud microservices architecture** with the following dependencies:

```
1. Config Server (Port 8888) [OPTIONAL]
   ↓
2. Service Registry/Eureka (Port 8761) [REQUIRED]
   ↓
3. Auth Service (Port 9000)
4. API Gateway (Port 8080)
5. Sample Service (other services)
```

## How to Start Services in Correct Order

### Option 1: Start with Full Infrastructure (Recommended)

#### Step 1: Start Config Server (Optional but recommended)
```cmd
cd C:\enterprise-skeleton
start cmd /k "mvnw.cmd spring-boot:run -pl config-server"
```
Wait ~30 seconds until you see "Started ConfigServerApplication"

#### Step 2: Start Service Registry (Eureka) - REQUIRED
```cmd
cd C:\enterprise-skeleton
start cmd /k "mvnw.cmd spring-boot:run -pl service-registry"
```
Wait ~30 seconds until you see "Started ServiceRegistryApplication"
Access: http://localhost:8761

#### Step 3: Start Auth Service
```cmd
cd C:\enterprise-skeleton
start cmd /k "mvnw.cmd spring-boot:run -pl auth-service"
```
Wait ~30 seconds until you see "Started AuthServiceApplication"
Access: http://localhost:9000
Swagger UI: http://localhost:9000/swagger-ui.html

#### Step 4: Start API Gateway
```cmd
cd C:\enterprise-skeleton
start cmd /k "mvnw.cmd spring-boot:run -pl api-gateway"
```
Wait ~30 seconds until you see "Started ApiGatewayApplication"
Access: http://localhost:8080

### Option 2: Start Services Standalone (Minimal)

If you want to run auth-service or api-gateway without the full infrastructure:

#### Disable Eureka Client (if you don't want to start Service Registry)

**For Auth Service** - Edit `auth-service/src/main/resources/application.yml`:
```yaml
eureka:
  client:
    enabled: false  # Add this line
    service-url:
      defaultZone: ${EUREKA_SERVICE_URL:http://eureka:eureka@localhost:8761/eureka/}
```

**For API Gateway** - Edit `api-gateway/src/main/resources/application.yml`:
```yaml
eureka:
  client:
    enabled: false  # Add this line
    service-url:
      defaultZone: ${EUREKA_SERVICE_URL:http://eureka:eureka@localhost:8761/eureka/}
```

Then rebuild and start:
```cmd
mvnw.cmd clean package -pl auth-service,api-gateway -am -DskipTests
```

## Quick Commands Reference

### Build All Services
```cmd
cd C:\enterprise-skeleton
mvnw.cmd clean package -DskipTests
```

### Build Specific Service
```cmd
mvnw.cmd clean package -pl auth-service -am -DskipTests
mvnw.cmd clean package -pl api-gateway -am -DskipTests
```

### Run Service Using Maven
```cmd
mvnw.cmd spring-boot:run -pl auth-service
mvnw.cmd spring-boot:run -pl api-gateway
```

### Run Service Using JAR
```cmd
cd auth-service
java -jar target\auth-service.jar

cd api-gateway
java -jar target\api-gateway.jar
```

### Fix Code Formatting Issues
```cmd
mvnw.cmd spotless:apply
```

## Default Credentials

### Auth Service
- **Admin User**: admin / admin (ROLE_ADMIN, ROLE_USER)
- **Regular User**: user / user (ROLE_USER)

### Config Server
- **Username**: admin
- **Password**: admin

### Service Registry (Eureka)
- **Username**: eureka
- **Password**: eureka

## Important Endpoints

### Auth Service (Port 9000)
- Login: `POST http://localhost:9000/api/auth/login`
- Refresh Token: `POST http://localhost:9000/api/auth/refresh`
- Swagger UI: http://localhost:9000/swagger-ui.html
- H2 Console: http://localhost:9000/h2-console
- Actuator: http://localhost:9000/actuator/health

### API Gateway (Port 8080)
- Routes all requests to backend services
- Actuator: http://localhost:8080/actuator/health
- Gateway Routes: http://localhost:8080/actuator/gateway/routes

### Service Registry (Port 8761)
- Dashboard: http://localhost:8761
- Applications: http://localhost:8761/eureka/apps

### Config Server (Port 8888)
- Health: http://localhost:8888/actuator/health
- Config for auth-service: http://localhost:8888/auth-service/default

## Troubleshooting

### Issue: "Connection refused" to Eureka
**Cause**: Service Registry is not running
**Solution**: Start service-registry first (see Step 2 above)

### Issue: "401" errors from Config Server
**Cause**: Config Server is not running or wrong credentials
**Solution**: Either start config-server OR it's optional (fail-fast: false)

### Issue: "Spotless formatting violations"
**Cause**: Code doesn't match configured formatting rules
**Solution**: Run `mvnw.cmd spotless:apply`

### Issue: Port already in use
**Cause**: Another process is using the port
**Solution**: 
```cmd
netstat -ano | findstr :9000
taskkill /F /PID <pid>
```

## Docker Compose Alternative

For easier management, you can use Docker Compose:
```cmd
cd C:\enterprise-skeleton
docker-compose up
```

## Next Steps

1. ✅ Code formatting issues - FIXED
2. ✅ Auth Service - RUNNING (with warnings)
3. ⏭️ Start Service Registry for full functionality
4. ⏭️ Start API Gateway
5. ⏭️ Test end-to-end authentication flow

---

**Last Updated**: May 4, 2026
**Status**: Auth Service is running standalone. Needs Service Registry for full microservices functionality.
