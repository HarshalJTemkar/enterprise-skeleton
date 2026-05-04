# Contributing

Thank you for considering a contribution! This project follows a
**conventional, trunk-based** workflow.

## Quick start

```bash
git clone <repo>
cd enterprise-skeleton
./mvnw -Pdev clean verify
```

Required tools: JDK 21 (Temurin), Maven 3.9+, Docker (for integration
tests / Testcontainers).

## Branching

* `main` is always releasable.
* Use short-lived feature branches: `feat/<topic>`, `fix/<topic>`,
  `chore/<topic>`, `docs/<topic>`.
* Squash-merge PRs.

## Commit messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(auth-service): add /logout endpoint
fix(common-lib): correct masking regex for Bearer tokens
docs(readme): clarify gateway routing
```

Allowed types: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`,
`build`, `ci`, `perf`, `revert`.

## Code style

* Java 21, `-Xlint:all` clean.
* `./mvnw spotless:apply` before committing.
* Public API requires Javadoc; classes annotated with
  `@AutoConfiguration` must document every property.
* Tests: AAA layout, `*Test` for unit, `*IT` for integration
  (Testcontainers).

## Quality gates

A PR is mergeable only if:

* `./mvnw -Pstaging verify` is green
* Coverage ≥ **70 %** line coverage on the affected module
  (`./mvnw -Pcoverage verify`)
* No new `HIGH`/`CRITICAL` Dependency-Check or Trivy findings
* Checkstyle, SpotBugs, PMD report no new violations

## Adding a new microservice

1. Copy `auth-service/` as a template and rename module/package.
2. Register it as a child module in the root `pom.xml`.
3. Add a config-server file: `config-server/src/main/resources/config/<name>.yml`.
4. Add a Kustomize overlay manifest under `kubernetes/base/<name>/`.
5. Document any new auto-configuration in `README.md`.

## Reporting bugs

Open a GitHub Issue with:

* Affected version / commit
* Steps to reproduce
* Expected vs. actual behavior
* Logs (with secrets redacted)

## License

By contributing you agree your work is released under the
[Apache 2.0](LICENSE) license.
