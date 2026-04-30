# 0002. Use Java 21 + Spring Boot 3.4 + Spring Cloud 2024

* Status: accepted
* Date: 2026-04-27
* Deciders: platform-team

## Context

The skeleton must remain on a release line that is supported for at
least 24 months and unlocks **virtual threads**, **records**,
**pattern matching**, and the modern reactive Spring Cloud Gateway.

## Decision

* Language baseline: **Java 21 LTS** (`<release>21</release>`).
* Framework: **Spring Boot 3.4.x** (Jakarta EE 10).
* Cloud: **Spring Cloud 2024.0.x** (Eureka client, Config, Gateway).
* Virtual threads enabled globally via
  `spring.threads.virtual.enabled=true`.

## Consequences

* Drops support for Java 17 and Spring Boot 2.x.
* All third-party libraries must be Jakarta-namespace compatible.
* Reactive `WebFlux` is used **only** in `api-gateway`; all other
  services remain on `Servlet` MVC for simplicity.

## Alternatives considered

* **Java 17 + Spring Boot 3.2** — already in EOL window, no virtual
  threads.
* **Quarkus / Micronaut** — out of scope for a Spring-centric skeleton.
