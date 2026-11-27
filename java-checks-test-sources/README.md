---
title: SonarJava rule test sources
description: This reactor supplies sample sources and classpaths for rule tests and integration tests.
generated:
  by: tirion
---

# SonarJava rule test sources

Read [Rule tests and sample placement](../docs/systems/rule-tests.md) before adding or moving a sample. That guide covers the build JDK, module choice, and verifier setup.

After [toolchain setup](../docs/systems/build.md), run `mise exec java@26 -- mvn -f java-checks-test-sources/default/pom.xml clean install sonar:sonar -Panalyze-tests` from the repository root to analyze the default samples on a local SonarQube instance. To compare scanner behavior with and without compiled classes, use the corresponding settings file in that module:

- `java-checks-test-sources/default/sonar-project-with-binaries.properties`
- `java-checks-test-sources/default/sonar-project-no-binaries.properties`
