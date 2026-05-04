# Security Policy

## Supported versions

Only the latest `main` branch and the most recent minor release of
`enterprise-skeleton` receive security patches.

| Version           | Supported          |
|-------------------|--------------------|
| `main` / `1.x`    | :white_check_mark: |
| `< 1.0`           | :x:                |

## Reporting a vulnerability

**Please do not open public GitHub issues for security problems.**

1. Email `security@enterprise.example` (PGP key fingerprint published in
   `docs/security/pgp.txt`).
2. Include a clear description, reproduction steps, affected version /
   commit, and any suggested mitigation.
3. You will receive an acknowledgement within **2 business days** and a
   triage update within **7 business days**.

## Disclosure process

* We will agree on a coordinated disclosure date with the reporter.
* A CVE will be requested via GitHub Security Advisories where applicable.
* Patches are released to all supported versions before public disclosure.

## Hardening checklist (operators)

* Always override `enterprise.common.jwt.secret` via env / Vault — never
  rely on the dev default.
* Run with the `prod` Maven profile (`-Pprod`) so quality gates and source
  artifacts are enforced.
* Pin container digests in production manifests (`image: …@sha256:…`).
* Subscribe to Dependabot / Renovate alerts for this repository.
* Enable image scanning (Trivy/Grype) in your registry; the CI pipeline
  already fails on `HIGH`/`CRITICAL` CVEs.
