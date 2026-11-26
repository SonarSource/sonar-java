---
title: SonarJava public API and downstream consumers
description: Preserve the contracts used by external check providers and verify custom-rule compatibility when changing the Java analyzer API.
generated:
  by: tirion
---

# SonarJava public API and downstream consumers

`java-frontend/src/main/java/org/sonar/plugins/java/api` contains the contracts for writing and registering Java checks. Consumer plugins compile and load separately from SonarJava. Custom-rule plugins and separately maintained symbolic-execution, DBD and security analyzers can depend on these contracts. Java's `public` modifier alone does not make an implementation class a supported extension point; do not expose internal parser or semantic-model classes to solve a rule-specific problem.

## Evolve the API without breaking consumers

- Preserve source and binary compatibility for supported APIs. For example, adding an abstract method to an interface forces existing implementations to change. Prefer a compatible default method or additive API where the semantics allow it.
- Do not remove or change a supported signature without a deprecation path. Introduce the replacement, mark the old API deprecated, document how callers should migrate, and keep the old behavior during the transition. Check `@Beta` and `internal` boundaries before relying on a class or promising it to downstream users.
- Review behavioral changes as well as signatures. Changes to callback order, when symbols are available, or what a scanner sees on unchanged files can break external rules without a compile error.

`docs/java-custom-rules-example` is a buildable consumer, not a substitute for downstream compatibility review. The **Custom Rules and License Check** job in `.github/workflows/build.yml` builds the example with `docs/java-custom-rules-example/pom_SQ_10_6_LATEST.xml`, which pins older SonarJava and SonarQube plugin APIs. Keep the example working on that baseline; a successful build against the pinned dependencies does not prove that separately built plugins will load with a changed analyzer. Review dependencies and runtime behavior with the affected downstream owners for a contract change.

## Extension hooks

- `java-frontend/src/main/java/org/sonar/plugins/java/api/CheckRegistrar.java` registers checks from another plugin, separating checks for main and test sources. The sample implementation is `docs/java-custom-rules-example/src/main/java/org/sonar/samples/java/MyJavaFileCheckRegistrar.java`.
- `java-frontend/src/main/java/org/sonar/plugins/java/api/ProfileRegistrar.java` contributes rule keys to built-in profiles. `sonar-java-plugin/src/main/java/org/sonar/plugins/java/BuiltInJavaQualityProfile.java` combines packaged profile files and registrar contributions, with a legacy fallback for Security and DBD rules. For built-in checks declared in this repository, see [Rule metadata and profiles](rule-metadata.md).
- `sonar-java-plugin/src/main/java/org/sonar/plugins/java/JavaPlugin.java` installs `SonarLintCache` for `SonarProduct.SONARLINT`: DBD uses it to transfer in-memory data between plugins. It is not persistent between analyses and does not replace the server-side incremental cache. Preserve the extension when changing product-specific wiring.
