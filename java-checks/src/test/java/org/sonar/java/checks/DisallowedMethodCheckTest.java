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

class DisallowedMethodCheckTest {

  @Test
  void detected() {
    DisallowedMethodCheck disallowedMethodCheck = new DisallowedMethodCheck();
    disallowedMethodCheck.setClassName("A");
    disallowedMethodCheck.setMethodName("foo");
    disallowedMethodCheck.setArgumentTypes("int, long, java.lang.String[]");
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DisallowedMethodCheck/detected.java")
      .withCheck(disallowedMethodCheck)
      .verifyIssues();
  }

  @Test
  void all_overloads() {
    DisallowedMethodCheck disallowedMethodCheck = new DisallowedMethodCheck();
    disallowedMethodCheck.setClassName("A");
    disallowedMethodCheck.setMethodName("foo");
    disallowedMethodCheck.setAllOverloads(true);
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DisallowedMethodCheck/detected.java")
      .withCheck(disallowedMethodCheck)
      .verifyIssues();
  }

  @Test
  void empty_parameters() {
    DisallowedMethodCheck disallowedMethodCheck = new DisallowedMethodCheck();
    disallowedMethodCheck.setClassName("A");
    disallowedMethodCheck.setMethodName("bar");
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DisallowedMethodCheck/empty_parameters.java")
      .withCheck(disallowedMethodCheck)
      .verifyIssues();
  }

  @Test
  void empty_type_definition() {
    DisallowedMethodCheck disallowedMethodCheck = new DisallowedMethodCheck();
    disallowedMethodCheck.setMethodName("bar");
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DisallowedMethodCheck/empty_type_definition.java")
      .withCheck(disallowedMethodCheck)
      .verifyIssues();
  }

  @Test
  void empty_method_name() {
    DisallowedMethodCheck disallowedMethodCheck = new DisallowedMethodCheck();
    disallowedMethodCheck.setClassName("A");
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/DisallowedMethodCheck/empty_method_name.java"))
      .withCheck(disallowedMethodCheck)
      .verifyNoIssues();
  }
}
