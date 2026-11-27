---
name: new-rule
description: Implement a new SonarJava analyzer rule from its RSPEC specification and verify its behavior with rule tests and CI ruling.
metadata:
  generated.by: tirion
---

# Implement a new SonarJava rule

1. **Generate rule inputs.** Confirm the rule key, title and RSPEC branch before starting. Follow `docs/systems/rule-metadata.md` to generate the descriptors and review profile membership. Do not invent metadata if the specification is unavailable.
2. **Write a reproducer.** Follow [Rule tests and sample placement](../../../docs/systems/rule-tests.md) to choose a fixture module and [Rule test assertions](#rule-test-assertions) to write cases and mark issues. Add a minimal check with the rule key and confirm that the test fails because it expected a finding. Also include compliant lookalikes and cases where information is missing.
3. **Implement the check.** Follow `docs/systems/rule-api.md` to choose the visitor and match Java constructs. If the check needs cross-file evidence or a quick fix, read `docs/systems/project-level-rules.md` or `docs/systems/quickfix-tests.md` respectively. Run the targeted verifier test and compile changed sample sources.
4. **Validate on real projects.** When CI results are available, follow `docs/systems/ruling-tests.md` to review the generated baseline and evaluate precision. Add false positives as regression cases, refine the check, and review the next CI results rather than accepting unexpected findings.

## Rule test assertions

Use JUnit Jupiter and `java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/CheckVerifier.java`. Cover triggering and similar-looking compliant cases. Every new rule needs a semantic test and a `withoutSemantic()` test. Call `verifyIssues()` when you expect findings and `verifyNoIssues()` when you expect none. In the nonsemantic test, assert any findings the rule can still report safely from syntax alone, but do not assume unresolved types. Share sample-path constants when tests load the same file. For rules gated on a library version, test absent or incompatible dependencies separately; `withoutSemantic()` does not simulate dependency version discovery.

See `java-checks/src/test/java/org/sonar/java/checks/DurationTimeUnitAgreementCheckTest.java` for a rule that needs resolution, and `java-checks/src/test/java/org/sonar/java/checks/AbsOnNegativeCheckTest.java` for a rule that reports in both modes.

Mark each expected issue with `// Noncompliant {{Expected message.}}`. On the line directly below each reported location, align carets under the exact span, for both primary and secondary locations. Add `<` after the secondary carets when the primary is above, or `>` when it is below:

```java
    new Thread(() -> {
//      ^^^^^^>
      Thread.sleep(1000); // Noncompliant {{Do not sleep here.}}
//           ^^^^^
    });
```

If carets cannot express the location (for example, a span at column 1 or across multiple lines), use explicit range attributes such as `sc`, `el`, and `ec`, or the `secondary` attribute to assert secondary lines. Use relative line offsets where possible.
