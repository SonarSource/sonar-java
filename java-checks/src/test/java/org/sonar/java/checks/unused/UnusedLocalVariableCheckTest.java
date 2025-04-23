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
import org.sonar.java.checks.verifier.TestUtils;

class UnusedLocalVariableCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/unused/UnusedLocalVariableCheck.java"))
      .withCheck(new UnusedLocalVariableCheck())
      .withJavaVersion(22)
      .verifyIssues();
  }

  @Test
  void test_java22() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/unused/UnusedLocalVariableCheck_java22.java"))
      .withCheck(new UnusedLocalVariableCheck())
      .withJavaVersion(22)
      .verifyIssues();
  }

  /** Check that issue that can only be acted upon with Java 22 are not raised for earlier versions. */
  @Test
  void test_java21() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/unused/UnusedLocalVariableCheck_java22.java"))
      .withCheck(new UnusedLocalVariableCheck())
      .withJavaVersion(21)
      .verifyNoIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/unused/UnusedLocalVariableCheck.java"))
      .withCheck(new UnusedLocalVariableCheck())
      .withJavaVersion(22)
      .verifyIssues();
  }

  @Test
  void test_with_lambda() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/unused/UnusedLocalVariableCheck_withLambda.java"))
      .withCheck(new UnusedLocalVariableCheck())
      .withJavaVersion(22)
      .verifyIssues();
  }

  /**
   * Test for false negative when a name is used in a lambda expression.
   * See SONARJAVA-5504 for details.
   */
  @Test
  void test_with_lambda_without_semantics() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/unused/UnusedLocalVariableCheck_withLambda.java"))
      .withCheck(new UnusedLocalVariableCheck())
      .withJavaVersion(22)
      .withoutSemantic()
      // False negatives.
      .verifyNoIssues();
  }
}
