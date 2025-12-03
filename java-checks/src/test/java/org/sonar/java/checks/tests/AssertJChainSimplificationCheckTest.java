/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.checks.tests;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testCodeSourcesPath;

class AssertJChainSimplificationCheckTest {
  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertJChainSimplificationCheckTest.java"))
      .withCheck(new AssertJChainSimplificationCheck())
      .verifyIssues();
  }

  @Test
  void testJava11Cases() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/tests/AssertJChainSimplificationCheckTestJava11.java"))
      .withCheck(new AssertJChainSimplificationCheck())
      .verifyIssues();
  }

  @Test
  void test_quick_fixes() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertJChainSimplificationCheckTest_QuickFix.java"))
      .withCheck(new AssertJChainSimplificationCheck())
      .verifyIssues();
  }

  @Test
  void test_list_quick_fixes() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertJChainSimplificationCheckTest_ListQuickFix.java"))
      .withCheck(new AssertJChainSimplificationCheck())
      .verifyIssues();
  }

}
