---
title: Feature-flag applicability
description: Decide whether a SonarJava behavior change needs independent activation and rollback.
sources:
  - resource: "5742002202"
generated:
  by: tirion
---

# Feature-flag applicability

Record in the PR whether a flag is applicable and why. Documentation, build-only maintenance and correctness fixes that should apply wherever the artifact is used normally do not need new runtime flags. For a new rule, review [profile membership](rule-metadata.md) separately from implementing the rule: inclusion in a built-in profile affects default activation; it is not a general-purpose runtime flag.

Consider a flag for a behavior change that needs independent activation or isolation from the rest of a plugin release, such as a new parser or analysis path with compatibility or performance risk. `java-frontend/src/main/java/org/sonar/java/SonarComponents.java` reads scanner settings for analysis behavior, and `sonar-java-plugin/src/main/java/org/sonar/plugins/java/JavaPlugin.java` registers supported public settings. They show existing configuration, not a mandate to expose a public setting for every rollout. Agree on the activation mechanism with the owners and check applicability on Server, Cloud and IDE separately.

For a flag, document its default, owner, activation approval, rollout stages, signals to monitor, tested fallback and removal criteria. Test the existing path as well as the new path. If no flag is appropriate, state how the change ships and how to recover, such as reverting the change and validating the replacement candidate.
