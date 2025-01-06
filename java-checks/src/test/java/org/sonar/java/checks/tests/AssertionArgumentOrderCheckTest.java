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
package org.sonar.java.checks.tests;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.testCodeSourcesPath;

class AssertionArgumentOrderCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionArgumentOrderCheckSample.java"))
      .withCheck(new AssertionArgumentOrderCheck())
      .verifyIssues();
  }

  @Test
  void test_JUnit5() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionArgumentOrderCheck_JUnit5.java"))
      .withCheck(new AssertionArgumentOrderCheck())
      .verifyIssues();
  }

  @Test
  void test_TestNG() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionArgumentOrderCheck_TestNG.java"))
      .withCheck(new AssertionArgumentOrderCheck())
      .verifyIssues();
  }

  @Test
  void quickfixes() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionArgumentOrderCheck_QuickFixes.java"))
      .withCheck(new AssertionArgumentOrderCheck())
      .verifyIssues();
  }

}
