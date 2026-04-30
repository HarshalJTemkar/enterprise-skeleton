# 0001. Record architecture decisions

* Status: accepted
* Date: 2026-04-27
* Deciders: platform-team

## Context

We need a lightweight, version-controlled way to capture the *why*
behind architecture decisions in this skeleton so that future
contributors do not have to reverse-engineer them from code.

## Decision

We adopt **Architecture Decision Records (ADRs)** as proposed by
Michael Nygard. Records live under `docs/adr/`, are sequentially
numbered, written in Markdown, and follow `0000-template.md`.

## Consequences

* All non-trivial architecture/security/tooling choices must land with
  a matching ADR in the same PR.
* ADRs are immutable once accepted; supersede via a new ADR.
* Tooling: optional [`adr-tools`](https://github.com/npryce/adr-tools)
  may be used (`adr new "Title"`).

## Alternatives considered

* **Wiki only** — too easy to drift from code, not reviewable.
* **Confluence** — gated behind SSO, poor PR-time review.
