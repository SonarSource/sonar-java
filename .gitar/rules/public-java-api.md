---
title: Preserve the Java analyzer public API
description: Check that incompatible changes have an explicit deprecation and migration path.
type: check
when: Pull requests change java-frontend/src/main/java/org/sonar/plugins/java/api/ outside its internal package.
actions: Report confirmed compatibility breaks without a deprecation path in PR Verification.
---

# Preserve the Java analyzer public API

Read `docs/systems/public-api.md`. Compare public signatures and contracts to the base branch. Flag removals, incompatible changes to existing implementations, and newly required methods on public interfaces unless there is a reviewed compatibility and migration path. Do not flag internal-only edits, compatible additions, or documentation-only changes. Point to the affected API and likely consumer impact.
