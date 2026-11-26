---
name: new-rule
description: Implement a new SonarJava analyzer rule from its RSPEC specification and verify its behavior with rule tests and CI ruling.
metadata:
  generated.by: tirion
---

# Implement a new SonarJava rule

1. **Generate rule inputs.** Confirm the rule key, title and RSPEC branch before starting. Follow `docs/systems/rule-metadata.md` to generate the descriptors and review profile membership. Do not invent metadata if the specification is unavailable.
2. **Write a reproducer.** Follow `docs/systems/rule-tests.md` for sample placement, semantic and nonsemantic cases, and issue locations. Add a minimal check with the rule key and confirm that the test fails because it expected a finding. Also include compliant lookalikes and cases where information is missing.
3. **Implement the check.** Follow `docs/systems/rule-api.md` to choose the visitor and match Java constructs. If the check needs cross-file evidence or a quick fix, read `docs/systems/project-level-rules.md` or `docs/systems/quickfix-tests.md` respectively. Run the targeted verifier test and compile changed sample sources.
4. **Validate on real projects.** When CI results are available, follow `docs/systems/ruling-tests.md` to review the generated baseline and evaluate precision. Add false positives as regression cases, refine the check, and review the next CI results rather than accepting unexpected findings.
