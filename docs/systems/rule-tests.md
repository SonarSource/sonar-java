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

## Write verifier assertions

Follow the [new-rule skill](../../.agents/skills/new-rule/SKILL.md#rule-test-assertions) for test cases, verifier methods, and primary and secondary location markers. See [Quick-fix tests](quickfix-tests.md) for fix declarations that accompany an issue marker.

## Verify

From the repository root after [toolchain setup](build.md), run the targeted test and its prerequisite reactor modules. Substitute the test class for the example:

```bash
mise run test:unit --module java-checks --test DurationTimeUnitAgreementCheckTest
```

To compile the shared samples as in CI, run from the root:

```bash
mise exec java@26 -- mvn -f java-checks-test-sources/pom.xml clean compile
```

Use `test-compile` instead of `compile` when changing test-scope samples.
