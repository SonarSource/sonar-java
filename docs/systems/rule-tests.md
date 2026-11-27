---
title: Rule tests and sample placement
description: Test positive and negative cases with and without semantics, and put samples in the module that supplies their classpath.
generated:
  by: tirion
---

# Rule tests and sample placement

## Choose where the sample belongs

`java-checks-test-sources/pom.xml` builds the shared rule samples. Use JDK 26 to build the sample reactor; the default sample module also sets its compiler release in `java-checks-test-sources/default/pom.xml`.

- Compiling samples for main-code checks: `java-checks-test-sources/default/src/main/java/checks`, loaded with `TestUtils.mainCodeSourcesPath`. Use `package checks;` and a distinct top-level type name to avoid conflicts with other samples.
- Compiling samples for test-code checks: `java-checks-test-sources/default/src/test/java/checks`, loaded with `TestUtils.testCodeSourcesPath`.
- Deliberately non-compiling samples: `java-checks-test-sources/default/src/main/files/non-compiling/checks`, loaded with `TestUtils.nonCompilingTestSourcesPath`. Use this for intentionally broken sources or missing bytecode, not as a shortcut around compiling a regular sample.
- Java-version or framework-specific samples belong in an existing module under `java-checks-test-sources` that provides the correct compiler or dependencies. Use the corresponding `TestUtils` helper ending in `InModule` and configure the verifier classpath or Java version when the test needs it. Selecting a sample path does not itself change the verifier classpath.

For the default main-code case, name the sample after the check with a **Sample.java** suffix. Keep parser-only and other dedicated ad hoc fixtures under `java-checks/src/test/files` when they do not belong to a Maven sample module; do not add ordinary new rule samples to the legacy `java-checks/src/test/files/checks` directory.

Reuse the prepared test dependencies or use focused stubs. Do not add dependencies just to exercise an API when the existing test reactor can model it.

## Assert both analysis modes

Use JUnit Jupiter (see [JUnit 5](../adrs/junit-5.md)) and `java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/CheckVerifier.java`. Every new rule needs a semantic test and a `withoutSemantic()` test. Resolution can fail when code is incomplete or the classpath is missing; check that the rule neither crashes nor speculates on unresolved types. Use `verifyNoIssues()` if the rule needs resolution, or `verifyIssues()` with the expected remaining findings if it can safely report from syntax alone.

For a concrete pair, see `java-checks/src/test/java/org/sonar/java/checks/DurationTimeUnitAgreementCheckTest.java`. Share sample-path constants when tests load the same sample. If behavior depends on a library version, add a case for an absent or incompatible dependency as well as the supported one; `withoutSemantic()` alone does not simulate a change to dependency version discovery.

## Mark expected issues in the sample

Cover both triggering and similar-looking compliant cases. Put `// Noncompliant {{Expected message.}}` on the line with an expected issue. Put carets directly below to assert the primary range; a bare marker cannot catch a regression that moves highlighting to the wrong expression. Mark secondary ranges with carets and a direction marker: `<` points back to a primary above and `>` points forward to a primary below.

```java
    new Thread(() -> {
//      ^^^^^^>
      Thread.sleep(1000); // Noncompliant {{Do not sleep here.}}
//           ^^^^^
    });
```

If a caret cannot express the span (for example at column 1 or across multiple lines), use the explicit attributes such as `sc`, `el`, `ec` or `secondary`. Prefer relative lines to absolute lines so unrelated edits above the marker do not break the test. See [Quick-fix tests](quickfix-tests.md) for fix declarations that accompany an issue marker.

## Verify

From the repository root with the project dependencies and compiled samples available, run the targeted rule test; substitute the test class for the example:

```bash
mvn test -pl java-checks -Dtest=DurationTimeUnitAgreementCheckTest
```

From `java-checks-test-sources`, `mvn clean compile` compiles the shared samples as in CI. It does not compile test-scope samples; use `mvn test-compile` from the same directory when changing those.
