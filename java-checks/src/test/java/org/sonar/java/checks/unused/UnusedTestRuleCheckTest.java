/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import static org.sonar.java.checks.verifier.TestUtils.testCodeSourcesPath;

class UnusedTestRuleCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/UnusedTestRuleCheck.java"))
      .withCheck(new UnusedTestRuleCheck())
      .verifyIssues();
  }

  @Test
  void test_JUnit5() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/UnusedTestRuleCheck_JUnit5.java"))
      .withCheck(new UnusedTestRuleCheck())
      .verifyIssues();
  }

  @Test
  void test_UseProtected() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/UnusedTestRuleCheck_Protected.java"))
      .withCheck(new UnusedTestRuleCheck())
      .verifyIssues();
  }

  @Test
  void test_no_issues_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/UnusedTestRuleCheck_JUnit5.java"))
      .withCheck(new UnusedTestRuleCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

}
