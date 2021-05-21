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

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

class AbstractClassNoFieldShouldBeInterfaceCheckTest {

  private static final String TEST_FILE = "src/test/files/checks/AbstractClassNoFieldShouldBeInterfaceCheck.java";

  @Test
  void test_no_version() {
    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/checks/AbstractClassNoFieldShouldBeInterfaceCheck_no_version.java")
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .verifyIssues();
  }

  @Test
  void test_with_java_7() {
    JavaCheckVerifier.newVerifier()
      .onFile(TEST_FILE)
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .withJavaVersion(7)
      .verifyNoIssues();
  }

  @Test
  void test_with_java_8() {
    JavaCheckVerifier.newVerifier()
      .onFile(TEST_FILE)
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .withJavaVersion(8)
      .verifyIssues();
  }

  @Test
  void test_with_java_9() {
    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/checks/AbstractClassNoFieldShouldBeInterfaceCheck_java9.java")
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .withJavaVersion(9)
      .verifyIssues();
  }

  @Test
  void test_no_version_without_semantics() {
    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/checks/AbstractClassNoFieldShouldBeInterfaceCheck_no_version.java")
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .withClassPath(Collections.emptyList())
      .verifyIssues();
  }

  @Test
  void test_with_java_7_without_semantics() {
    JavaCheckVerifier.newVerifier()
      .onFile(TEST_FILE)
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .withJavaVersion(7)
      .withClassPath(Collections.emptyList())
      .verifyNoIssues();
  }

  @Test
  void test_with_java_8_without_semantics() {
    JavaCheckVerifier.newVerifier()
      .onFile(TEST_FILE)
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .withJavaVersion(8)
      .withClassPath(Collections.emptyList())
      .verifyIssues();
  }


  @Test
  void test_with_java_9_without_semantics() {
    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/checks/AbstractClassNoFieldShouldBeInterfaceCheck_java9.java")
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .withJavaVersion(9)
      .withClassPath(Collections.emptyList())
      .verifyIssues();
  }
}
