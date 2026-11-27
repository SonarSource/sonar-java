---
name: migrate-test-samples
description: Move legacy SonarJava rule fixtures into the appropriate Maven sample module and update the verifier paths without changing rule behavior.
metadata:
  generated.by: tirion
---

# Migrate legacy rule test samples

Read `docs/systems/rule-tests.md` for destinations, package conventions and verifier helpers. This skill changes fixture placement, not the check or its expected findings.

1. Find the named check's test class under `java-checks/src/test/java/org/sonar/java/checks`. Trace every path it uses under `java-checks/src/test/files/checks`, including paths assembled from constants and shared fixtures. Find any other consumers before moving a shared file.
2. Decide which samples compile, which are deliberately non-compiling, and whether any are test code or need a version-specific module. Inspect the fixture and verifier setup before choosing; ask if the intended setup is ambiguous. Do not move a parser-only fixture just because it lives in the legacy tree.
3. Move the main fixture with the **CheckSample.java** suffix and keep meaningful suffixes on variants. For a compiling fixture entering `checks`, add `package checks;` if missing. For a non-compiling fixture, keep the original package or lack of one. Preserve issue annotations, source constructs and class names unless a compile fix is explicitly in scope. If a public class name or conflicting type prevents compilation, report the required change rather than switching the fixture to the non-compiling directory.
4. Replace legacy paths with the corresponding `TestUtils` helper using a path relative to its source root. Preserve classpath, Java version, file status and expected issue configuration. Update every reference to the moved file.
5. Run the affected test classes and compile the destination module for compiling samples. If a test or build fails, report the failure and what it would take to fix it. Do not rewrite fixture behavior as part of the move.
