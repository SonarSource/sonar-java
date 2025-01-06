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
package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class SuppressWarningsCheckTest {

  @Test
  void empty_list_of_warnings_then_any_suppressWarnings_is_an_issue() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/SuppressWarningsCheck/test1.java")
      .withCheck(getCheck(""))
      .verifyIssues();
  }

  @Test
  void list_of_warnings_with_syntax_error_then_any_suppressWarnings_is_an_issue() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/SuppressWarningsCheck/test1.java")
      .withCheck(getCheck("   ,   , ,,"))
      .verifyIssues();
  }

  @Test
  void only_one_warning_is_not_allowed() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/SuppressWarningsCheck/only_one_warning_is_not_allowed.java")
      .withCheck(getCheck("all"))
      .verifyIssues();
  }

  @Test
  void warning_based_on_constants_are_ignored() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/SuppressWarningsCheck/warning_based_on_constants_are_ignored.java")
      .withCheck(getCheck("boxing"))
      .verifyIssues();
  }

  @Test
  void two_warnings_from_different_lines_are_not_allowed() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/SuppressWarningsCheck/two_warnings_from_different_lines_are_not_allowed.java")
      .withCheck(getCheck("unused, cast"))
      .verifyIssues();
  }

  @Test
  void former_squid_repository_keys_are_still_supported() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/SuppressWarningsCheck/former_squid_rule_keys.java"))
      .withCheck(getCheck("squid:S1068, java:S115"))
      .verifyIssues();
  }

  private static SuppressWarningsCheck getCheck(String parameter) {
    SuppressWarningsCheck check = new SuppressWarningsCheck();
    check.warningsCommaSeparated = parameter;
    return check;
  }
}
