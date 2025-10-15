/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.unused;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class UnusedPrivateFieldCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/unused/UnusedPrivateFieldCheck.java"))
      .withCheck(new UnusedPrivateFieldCheck())
      .verifyIssues();
  }

  /**
   * Unused private fields can be detected without semantics. The test verifies
   * that we correctly process annotations which suppress warnings and
   * do not produce FPs when semantics is missing.
   */
  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/unused/UnusedPrivateFieldCheck.java"))
      .withCheck(new UnusedPrivateFieldCheck())
      .withoutSemantic()
      .verifyIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/unused/UnusedPrivateFieldCheck.java"))
      .withCheck(new UnusedPrivateFieldCheck())
      .verifyIssues();
  }

  @Test
  void test_native() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/unused/UnusedPrivateFieldCheckWithNative.java"))
      .withCheck(new UnusedPrivateFieldCheck())
      .verifyNoIssues();
  }

  @Test
  void test_quick_fixes() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/unused/UnusedPrivateFieldCheckWithQuickfixes.java"))
      .withCheck(new UnusedPrivateFieldCheck())
      .verifyIssues();
  }

  @Test
  void test_ignored_annotation() {
    UnusedPrivateFieldCheck check = new UnusedPrivateFieldCheck();
    check.ignoreAnnotations = "javax.inject.Inject,,javax.annotation.Nullable";
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/unused/UnusedPrivateFieldCheckWithIgnoredAnnotation.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void test_lombok_annotations() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/unused/UnusedPrivateFieldLombok.java"))
      .withCheck(new UnusedPrivateFieldCheck())
      .verifyIssues();
  }

  @Test
  void should_not_raise_when_referenced_in_annotation() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/unused/UnusedPrivateFieldCheckShouldNotRaiseWhenReferencedInAnnotation.java"))
      .withCheck(new UnusedPrivateFieldCheck())
      .verifyIssues();
  }
}
