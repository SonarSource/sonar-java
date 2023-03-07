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
package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class SuppressWarningsCheckTest {

  @Test
  void empty_list_of_warnings_then_any_suppressWarnings_is_an_issue() throws Exception {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/SuppressWarningsCheck/test1.java")
      .withCheck(getCheck(""))
      .verifyIssues();
  }

  @Test
  void list_of_warnings_with_syntax_error_then_any_suppressWarnings_is_an_issue() throws Exception {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/SuppressWarningsCheck/test1.java")
      .withCheck(getCheck("   ,   , ,,"))
      .verifyIssues();
  }

  @Test
  void only_one_warning_is_not_allowed() throws Exception {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/SuppressWarningsCheck/only_one_warning_is_not_allowed.java")
      .withCheck(getCheck("all"))
      .verifyIssues();
  }

  @Test
  void warning_based_on_constants_are_ignored() throws Exception {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/SuppressWarningsCheck/warning_based_on_constants_are_ignored.java")
      .withCheck(getCheck("boxing"))
      .verifyIssues();
  }

  @Test
  void two_warnings_from_different_lines_are_not_allowed() throws Exception {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/SuppressWarningsCheck/two_warnings_from_different_lines_are_not_allowed.java")
      .withCheck(getCheck("unused, cast"))
      .verifyIssues();
  }

  @Test
  void former_squid_repository_keys_are_still_supported() throws Exception {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/SuppressWarningsCheck/former_squid_rule_keys.java"))
      .withCheck(getCheck("squid:S1068, java:S115"))
      .verifyIssues();
  }

  private static SuppressWarningsCheck getCheck(String parameter) {
    SuppressWarningsCheck check = new SuppressWarningsCheck();
    check.warningsCommaSeparated = parameter;
    return check;
  }
}
