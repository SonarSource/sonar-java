/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks.security;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class ExcessiveContentRequestCheckTest {

  @Test
  void test_default_max() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck.java"))
      .withCheck(new ExcessiveContentRequestCheck())
      .verifyIssues();
  }

  @Test
  void test_spring_2_4() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/security/ExcessiveContentRequestCheck_spring_2_4.java"))
      .withCheck(new ExcessiveContentRequestCheck())
      .verifyIssues();
  }

  @Test
  void test_max_8_000_000() {
    ExcessiveContentRequestCheck check = new ExcessiveContentRequestCheck();
    check.fileUploadSizeLimit = 8_000_000L;
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck_max8000000.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void test_max_not_set() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck_sizeNotSet.java"))
      .withCheck(new ExcessiveContentRequestCheck())
      .verifyIssues();
  }

  @Test
  void test_max_set_in_another_file() {
    // As soon as the size is set somewhere in the project, do not report an issue.
    CheckVerifier.newVerifier()
      .onFiles(
        testSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck_setSize.java"),
        testSourcesPath("checks/security/ExcessiveContentRequestCheck/ExcessiveContentRequestCheck_sizeNotSet.java"))
      .withCheck(new ExcessiveContentRequestCheck())
      // Note that this will check that no issue îs reported on the second file (order is therefore important).
      .verifyNoIssues();
  }

}
