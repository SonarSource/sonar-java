---
title: Do not add abstract base classes for rules
description: Keep rules independent by sharing utilities rather than inheriting behavior from another rule implementation.
generated:
  by: tirion
---

# Do not add abstract base classes for rules

## Decision

Do not introduce abstract classes for checks to inherit from. Implement each rule on the analyzer's existing scanner API. Extract shared operations into utility classes or compose a helper with its own narrow responsibility.

## Rationale

Rules need to evolve independently as their detection criteria and exceptions change. An abstract rule base couples subscribers, traversal, reporting or state so a fix for one rule can change another rule. With composition, each caller opts into the shared behavior and keeps its own lifecycle.

## Alternatives

- **Shared abstract check:** reduces initial duplication but makes changes harder to isolate. Do not extend this pattern just because an existing family uses it.
- **Copy all logic into each rule:** preserves independence but leaves common semantic operations to drift. Share a utility when the behavior genuinely has the same contract for all callers.

See [Writing SonarJava rules](../systems/rule-api.md) for the framework visitor choices; reusing those framework classes is not the kind of inheritance prohibited here.
