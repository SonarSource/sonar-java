/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
  void test_no_issues_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/UnusedTestRuleCheck_JUnit5.java"))
      .withCheck(new UnusedTestRuleCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

}
