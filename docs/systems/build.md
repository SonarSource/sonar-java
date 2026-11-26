---
title: Setup, build and test commands
description: Build SonarJava and choose the appropriate test command and JDK.
generated:
  by: tirion
---

# Setup, build and test commands

`mise.toml` defines Maven 3.9 and Java 26, 21 and 17. Use JDK 26 for a full build, analyzer unit tests and compiling the test-source reactor. Use JDK 21 for Plugin QA and Ruling QA, as in `.github/workflows/build.yml`. `jdk.min.version` in `pom.xml` sets the plugin runtime minimum to 21, not the JDK needed to compile all test samples.

From the repository root, with [mise](https://mise.jdx.dev/) installed:

```bash
mise install
mise exec -- mvn clean install
```

For a focused rule test on a fresh checkout, include the upstream reactor modules. For another rule, replace `AbsOnNegativeCheckTest` with its actual test class.

```bash
mise exec -- mvn test -pl java-checks -am -Dtest=AbsOnNegativeCheckTest -Dsurefire.failIfNoSpecifiedTests=false
```

Compile the test-sample reactor and run the analyzer-wide crash check after installing the reactor artifacts. `sonar-java-plugin/src/test/java/org/sonar/plugins/java/SanityTest.java` exercises checks against test sources with a test scanner context, not a running SonarQube Server or the ruling corpus.

```bash
mise exec -- mvn -f java-checks-test-sources/pom.xml clean compile
mise exec -- mvn verify -f sonar-java-plugin/pom.xml -Psanity -Dtest=SanityTest
```

`pom.xml` keeps the JAR manifest's `Build-Time` constant so identical inputs remain eligible for Develocity cache hits. Do not restore a time-varying value.

## Integration tests

Build and install the plugin with JDK 26 first. Configure Orchestrator and Repox access before running integration tests. The [build workflow](../../.github/workflows/build.yml) loads credentials from Vault and passes `ORCHESTRATOR_CONFIG_URL` to Maven. For an authorized local run, set it to a file URL for your local Orchestrator properties file; never commit that file or its credentials. Where needed, set `MAVEN_HOME` and configure Orchestrator's local Maven repository through `MAVEN_LOCAL_REPOSITORY` or the `maven.localRepository` property.

External contributors and agents without commercial artifacts must use `-DcommunityEditionTestsOnly=true` to skip tests that require commercial editions. This does not grant Orchestrator or artifact access. Use the Plugin QA CI job if those prerequisites are unavailable.

```bash
mise exec java@21 -- mvn -f its/plugin/pom.xml clean install -Pit-plugin -DcommunityEditionTestsOnly=true
```

Do not run ruling locally for routine changes; see [ruling tests](ruling-tests.md) for the CI workflow and review procedure.
