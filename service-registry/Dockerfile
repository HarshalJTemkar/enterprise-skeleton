# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace
COPY . .
RUN java -Djarmode=layertools -jar target/service-registry.jar extract --destination target/extracted

FROM eclipse-temurin:21-jre-noble AS runtime
RUN groupadd --system app && useradd --system --gid app --home /app --shell /usr/sbin/nologin app \
    && apt-get update \
    && apt-get install -y --no-install-recommends curl tini ca-certificates \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
USER app:app

ARG EXTRACTED=target/extracted
COPY --chown=app:app ${EXTRACTED}/dependencies/         ./
COPY --chown=app:app ${EXTRACTED}/spring-boot-loader/   ./
COPY --chown=app:app ${EXTRACTED}/snapshot-dependencies/ ./
COPY --chown=app:app ${EXTRACTED}/application/          ./

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8761
EXPOSE 8761
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS -u $${EUREKA_USER:-eureka}:$${EUREKA_PASSWORD:-eureka} http://localhost:${SERVER_PORT}/actuator/health/liveness || exit 1
ENTRYPOINT ["/usr/bin/tini","--","sh","-c","exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
