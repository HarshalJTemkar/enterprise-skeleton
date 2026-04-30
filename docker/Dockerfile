# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY . .
# Expect that the JAR has already been built by `mvn -pl <module> -am package`.

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
ARG JAR_FILE=target/*.jar
RUN apk add --no-cache curl wget tini && addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY ${JAR_FILE} app.jar
RUN chown -R app:app /app
USER app

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["/sbin/tini", "--", "sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
