---
title: Write new tests in JUnit 5
description: Use JUnit Jupiter for new tests so that tests run under Maven Surefire as well as in the IDE.
generated:
  by: tirion
---

# Write new tests in JUnit 5

## Decision

Use JUnit Jupiter for new tests, including rule tests that use `CheckVerifier`. Import `org.junit.jupiter.api.Test`, not `org.junit.Test`. The forbiddenapis configurations in `java-checks/pom.xml` and `sonar-java-plugin/pom.xml` use `forbid_junit4.txt` to reject JUnit 4 signatures.

## Rationale

JUnit migration support can leave JUnit 4 types on the classpath. IntelliJ IDEA can still run a JUnit 4 test, while Surefire without the JUnit Vintage engine silently omits it. A successful IDE run does not prove that CI ran the test.

## Alternative

Running new tests on both engines would keep the ambiguity about which tests execute and preserve two sets of conventions. Existing Orchestrator harnesses can still use JUnit 4, as in `its/ruling/src/test/java/org/sonar/java/it/JavaRulingTest.java`; do not copy those harness conventions into unit tests.

See [Rule tests](../systems/rule-tests.md) for a verifier example.
