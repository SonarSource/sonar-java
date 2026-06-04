/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

class NoJUnit4AssertionsInJUnit5TestsCheckTest {
  @Test
  void test_junit4() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/NoJUnit4AssertionsInJUnit5TestsCheck_JUnit4SampleTest.java"))
      .withCheck(new NoJUnit4AssertionsInJUnit5TestsCheck())
      .verifyNoIssues();
  }

  @Test
  void test_junit5() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/NoJUnit4AssertionsInJUnit5TestsCheck_JUnit5SampleTest.java"))
      .withCheck(new NoJUnit4AssertionsInJUnit5TestsCheck())
      .verifyNoIssues();
  }

  @Test
  void test_junit5withJunit4assertions() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/NoJUnit4AssertionsInJUnit5TestsCheck_JUnit5WithNoncompliantAssertionsSampleTest.java"))
      .withCheck(new NoJUnit4AssertionsInJUnit5TestsCheck())
      .verifyIssues();
  }

  @Test
  void test_junit5withJunit4assertions_withoutSemantic() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/NoJUnit4AssertionsInJUnit5TestsCheck_JUnit5WithNoncompliantAssertionsSampleTest.java"))
      .withCheck(new NoJUnit4AssertionsInJUnit5TestsCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }
}
