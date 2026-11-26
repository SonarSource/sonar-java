---
title: Keep new rules stateless
description: Keep new file-level rules free of mutable scan state because rule instances are reused across files.
generated:
  by: tirion
---

# Keep new rules stateless

## Decision

New file-level checks must not store data from a scan in check fields. Use immutable constants and keep collections, counters and AST references local to callbacks or to a helper created for the current file. If a check needs to correlate nodes, start at their nearest enclosing method, class or compilation unit and collect the data locally.

Platform-injected `@RuleProperty` values and flags computed by `DependencyVersionAware` are configuration, not scan state. Do not mutate them during file analysis. A visitor that uses a field for `JavaFileScannerContext` must confine it to that scan; prefer a local visitor that receives the context.

When a rule truly requires module-wide evidence, isolate its aggregation from per-file traversal and follow [Project-level rules](../systems/project-level-rules.md).

## Rationale

`java-frontend/src/main/java/org/sonar/java/model/VisitorsBridge.java` uses the same scanners for multiple files. Scan data left in a rule can make findings depend on file order and retain parser or semantic objects after a file finishes.

## Alternative

Mutable fields plus `clearState()` are supported for existing `IssuableSubscriptionVisitor` checks. They are not the default for new checks: it is easy to add a field without adding it to cleanup, whereas callback-local data cannot leak into the next file. See [Writing SonarJava rules](../systems/rule-api.md) for maintaining existing stateful checks.
