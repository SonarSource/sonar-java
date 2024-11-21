/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

class AssertionCompareToSelfCheckTest {

  @Test
  void test_assertj() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionCompareToSelfCheck_Assertj.java"))
      .withCheck(new AssertionCompareToSelfCheck())
      .verifyIssues();
  }

  @Test
  void test_junit5() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionCompareToSelfCheck_JUnit5.java"))
      .withCheck(new AssertionCompareToSelfCheck())
      .verifyIssues();
  }

  @Test
  void test_junit4() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionCompareToSelfCheck_JUnit4.java"))
      .withCheck(new AssertionCompareToSelfCheck())
      .verifyIssues();
  }

  @Test
  void test_fest_assert() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionCompareToSelfCheck_FestAssert.java"))
      .withCheck(new AssertionCompareToSelfCheck())
      .verifyIssues();
  }

}
