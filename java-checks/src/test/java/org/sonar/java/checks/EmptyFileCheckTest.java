/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class EmptyFileCheckTest {

  @Test
  void test_empty_file() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/EmptyFile.java"))
      .withCheck(new EmptyFileCheck())
      .verifyIssueOnFile("This file has 0 lines of code.");
  }

  @Test
  void test_non_empty_file() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/NonEmptyFile.java"))
      .withCheck(new EmptyFileCheck())
      .verifyNoIssues();
  }

  @Test
  void with_package() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/WithPackage.java"))
      .withCheck(new EmptyFileCheck())
      .verifyNoIssues();
  }

  @Test
  void with_module() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/module/module-info.java")
      .withCheck(new EmptyFileCheck())
      .verifyNoIssues();
  }

}
