# SonarJava agent guide

## Scope

SonarJava provides Java parsing, semantic analysis and rules for SonarQube Server, SonarQube Cloud and SonarQube for IDE. Use `pom.xml` for the Maven reactor.
The ECJ fork, symbolic execution engine, DBD and security rules live outside this repository. Public API changes can break those consumers and customer custom-rule plugins.
All agent instructions start here; there are no component-level `AGENTS.md` files. Read the topic guides below for component-specific constraints.

## Baseline reference

[Repository Baseline](docs/systems/external-context.md#repository-baseline), checked 2026-11-26; shared `awesome-ai` template unavailable, so this file follows the guideline's required sections directly.

## Commands

Read [Setup, build and test commands](docs/systems/build.md) before selecting a JDK or running Maven. From the root, run `mise install` and `mise run doctor` to check which tasks can run. Use mise to run Maven, not the system installation.

## Constraints

- This repository is public. All agent files and docs are public. Do not publish internal URLs, customer data, credentials or private repository content.
- Preserve public API compatibility and SonarQube for IDE support; read `docs/systems/public-api.md` before changing the API or plugin registration.
- Use the supported build and runtime JDKs in `docs/systems/build.md`; CI exercises them for different purposes.
- The project uses SSALv1 (`LICENSE.txt`); read `docs/systems/licenses.md` before changing dependencies. Regenerate the bundled dependency licenses in `sonar-java-plugin/src/main/resources/licenses` instead of editing them by hand.
- Do not edit generated rule metadata or the generated check list by hand. Read `docs/systems/rule-metadata.md` before registering rules or changing profiles.
- New rules are stateless and must not introduce shared abstract check bases; read the decisions in `docs/adrs/index.md` before adding rule logic or unit tests.
- Use the [new-rule skill](.agents/skills/new-rule/SKILL.md#rule-test-assertions) for verifier assertions and issue markers. Read [rule tests and sample placement](docs/systems/rule-tests.md) before adding or moving a fixture. For an existing legacy example, see `java-checks/src/test/files/checks/AbsOnNegative.java`.
- Do not hand-edit ruling sources or change the pinned `its/sources` submodule revision as part of a rule change. Let CI run ruling and generate baseline updates; read `docs/systems/ruling-tests.md` before reviewing SARIF changes.

## Escalation

`@SonarSource/code-quality-jvm-squad` owns the repository, as defined in [CODEOWNERS](.github/CODEOWNERS). Use the [SONARJAVA Jira project](https://jira.sonarsource.com/browse/SONARJAVA/) for issue context. If RSPEC, Repox, Orchestrator or the ruling submodule is required and unavailable, ask the owners and report which check is blocked. Do not guess metadata, change baselines to hide failures or substitute credentials.

## Verification before merge

Read [verification before merge](docs/systems/verification.md) before requesting review; it covers local checks, CI jobs and changes requiring extra review.

## Feature-flag applicability

Read [feature activation](docs/systems/feature-activation.md) before changing behavior that may need independent rollout or rollback.

## Release checks

Read [release checks](docs/systems/release-checks.md) before preparing a release or changing release automation.

## Documentation

- Start with [the documentation index](docs/index.md); read `docs/documentation-conventions.md` before editing docs.
- Read `docs/systems/rule-api.md` before choosing a visitor or changing a rule's lifecycle.
- Read `docs/systems/project-level-rules.md` before changing cross-file analysis or cache behavior.
- Read `docs/systems/quickfix-tests.md` before adding or changing a quick fix.
- Use `.agents/skills/new-rule/SKILL.md` when implementing a rule from its specification.
- Use `.agents/skills/migrate-test-samples/SKILL.md` when moving legacy fixtures into the Maven test-source modules.

## External context

Read [external context](docs/systems/external-context.md) before looking up rule specifications, upstream release procedures, shared operations or design proposals.

---
