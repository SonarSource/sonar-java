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
      .onFile(testSourcesPath("checks/DisallowedMethodCheck/empty_method_name.java"))
      .withCheck(disallowedMethodCheck)
      .verifyNoIssues();
  }
}
