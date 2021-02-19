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

class ClassFieldCountCheckTest {

  private static final String TEST_FILES_DIR = "src/test/files/checks/ClassFieldCountCheck/";

  @Test
  void simple_case() {
    JavaCheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/ClassFieldCountCheck/SimpleDefaultCase.java"))
      .withCheck(new ClassFieldCountCheck())
      .verifyIssues();
    JavaCheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/ClassFieldCountCheck/SimpleDefaultCase.java"))
      .withCheck(new ClassFieldCountCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void static_final() {
    JavaCheckVerifier.newVerifier()
      .onFile(TEST_FILES_DIR + "ClassFieldCountCheck.java")
      .withCheck(new ClassFieldCountCheck())
      .verifyIssues();
  }

  @Test
  void enums_interfaces_and_anonymous_trees() {
    ClassFieldCountCheck check = new ClassFieldCountCheck();
    check.setThreshold(2);
    JavaCheckVerifier.newVerifier()
      .onFile(TEST_FILES_DIR + "UnusualTrees.java")
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void count_only_public_fields() {
    ClassFieldCountCheck check = new ClassFieldCountCheck();
    check.setCountNonPublicFields(false);
    JavaCheckVerifier.newVerifier()
      .onFile(TEST_FILES_DIR + "CountOnlyPublicFields.java")
      .withCheck(check)
      .verifyIssues();
  }

}
