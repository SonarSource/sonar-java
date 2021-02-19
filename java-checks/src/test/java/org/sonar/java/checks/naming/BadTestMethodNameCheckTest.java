/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks.naming;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class BadTestMethodNameCheckTest {

  @Test
  void test() {
    BadTestMethodNameCheck check = new BadTestMethodNameCheck();
    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/checks/naming/BadTestMethodNameCheck.java")
      .withCheck(check)
      .verifyIssues();
    // test with same instance to cover reuse of regexp pattern (lazy initialization).
    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/checks/naming/BadTestMethodNameCheck.java")
      .withCheck(check)
      .verifyIssues();

    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/checks/naming/BadTestMethodNameCheck.java")
      .withCheck(check)
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void test_with_customPattern() {
    BadTestMethodNameCheck check = new BadTestMethodNameCheck();
    check.format = "^test_sonar[A-Z][a-zA-Z0-9]*$";
    JavaCheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/naming/BadTestMethodNameCheckCustom.java"))
      .withCheck(check)
      .verifyIssues();
  }

}
