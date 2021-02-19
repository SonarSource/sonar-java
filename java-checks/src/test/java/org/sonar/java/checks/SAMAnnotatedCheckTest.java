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
package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class SAMAnnotatedCheckTest {

  private static final String FILE = "src/test/files/checks/SAMAnnotatedCheck.java";

  @Test
  void no_issue_with_no_java_version() {
    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/checks/SAMAnnotatedCheck_no_version.java")
      .withCheck(new SAMAnnotatedCheck())
      .verifyIssues();
    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/checks/SAMAnnotatedCheck_no_version.java")
      .withCheck(new SAMAnnotatedCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void test_java_8() {
    JavaCheckVerifier.newVerifier()
      .onFile(FILE)
      .withCheck(new SAMAnnotatedCheck())
      .withJavaVersion(8)
      .verifyIssues();
  }

  @Test
  void test_java_9() {
    JavaCheckVerifier.newVerifier()
      .onFile(FILE)
      .withCheck(new SAMAnnotatedCheck())
      .withJavaVersion(9)
      .verifyIssues();
    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/checks/SAMAnnotatedCheck_java9.java")
      .withCheck(new SAMAnnotatedCheck())
      .withJavaVersion(9)
      .verifyIssues();
  }

  @Test
  void no_issue_with_java_7() {
    JavaCheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/SAMAnnotatedCheck_java7.java"))
      .withCheck(new SAMAnnotatedCheck())
      .withJavaVersion(7)
      .verifyNoIssues();
  }
}
