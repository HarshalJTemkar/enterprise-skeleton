# 0004. Multi-stage layered Dockerfiles per service

* Status: accepted
* Date: 2026-04-27
* Deciders: platform-team

## Context

The original repo had **one** generic `docker/Dockerfile` reused by every
service. That created several issues:

* No build-context isolation — the whole repo had to be sent.
* No layered jar — every code change invalidated the dependency layer,
  blowing the image cache and slowing CI.
* No `HEALTHCHECK`, no non-root user, no `tini`.

## Decision

Each service ships its own `Dockerfile` and `.dockerignore`. The image:

* Uses **`eclipse-temurin:21-jre-noble`** (security-supported, ~ 90 MB).
* Extracts Spring Boot **layered jar** (`-Djarmode=layertools extract`)
  and copies the four layers (`dependencies`, `spring-boot-loader`,
  `snapshot-dependencies`, `application`) in cache-friendly order.
* Runs as non-root (`app:app`), `tini` is PID 1.
* Defines a `HEALTHCHECK` against `/actuator/health/liveness`.
* Honours `JAVA_OPTS` with sane container defaults
  (`-XX:MaxRAMPercentage=75.0`, `-XX:+ExitOnOutOfMemoryError`).

## Consequences

* CI image-build time drops dramatically when only application code changes.
* Trivy / cosign / SBOM steps are per-image and can fail-fast.
* The original `docker/Dockerfile` is kept for backwards-compatibility but
  marked deprecated.

## Alternatives considered

* **Buildpacks (`spring-boot:build-image`)** — simpler but less control
  over base image / non-root / scanning.
* **Distroless (`gcr.io/distroless/java21`)** — smaller, but no shell
  for `HEALTHCHECK`/`curl`. Reconsider when probes move to gRPC.
