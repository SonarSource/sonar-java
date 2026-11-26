---
title: SonarJava systems
description: Find the constraints and code entry points relevant to a SonarJava change.
generated:
  by: tirion
---

# SonarJava systems

## Building and delivering

- [Setup, build and test commands](build.md): select the right JDK and Maven invocation.
- [Verification before merge](verification.md): choose checks and interpret CI results.
- [Feature activation](feature-activation.md): decide whether a behavior change needs a rollout switch.
- [Release checks](release-checks.md): check the exact candidate version and release path.
- [Third-party licenses](licenses.md): regenerate and verify licenses after dependency changes.
- [External context](external-context.md): retrieve cross-repository or access-controlled information.

## Analysis and rules

- [Rule API](rule-api.md): choose a visitor and manage rule lifecycle.
- [Public Java analyzer API](public-api.md): preserve compatibility with external consumers.
- [Rule tests](rule-tests.md): place samples and choose semantic and no-semantic expectations.
- [Quick-fix tests](quickfix-tests.md): test edits as well as issue locations.
- [Project-level rules](project-level-rules.md): work with cross-file analysis and cache reuse.
- [Rule metadata and profiles](rule-metadata.md): update generated artifacts and profile membership.
- [Ruling tests](ruling-tests.md): review CI-generated SARIF changes.
