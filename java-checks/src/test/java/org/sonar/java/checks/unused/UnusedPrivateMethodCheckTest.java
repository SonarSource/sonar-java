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
package org.sonar.java.checks.unused;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.java.checks.verifier.internal.InternalCheckVerifier;

class UnusedPrivateMethodCheckTest {

  @Test
  void test() {
    InternalCheckVerifier.newInstance()
      .onFile(TestUtils.mainCodeSourcesPath("checks/UnusedPrivateMethod.java"))
      .withCheck(new UnusedPrivateMethodCheck())
      .withQuickFixes()
      .verifyIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/UnusedPrivateMethod.java"))
      .withCheck(new UnusedPrivateMethodCheck())
      .verifyIssues();
  }
  
  @Test
  void test_non_compiling_unknown() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/unused/UnusedPrivateMethodWithUknownResolution.java"))
      .withCheck(new UnusedPrivateMethodCheck())
      .verifyIssues();
  }

}
