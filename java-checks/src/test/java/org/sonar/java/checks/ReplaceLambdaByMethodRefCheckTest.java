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
import org.sonar.java.checks.verifier.internal.InternalCheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class ReplaceLambdaByMethodRefCheckTest {

  private static final String FILENAME = "checks/ReplaceLambdaByMethodRefCheck.java";
  public static final String NO_VERSION_FILENAME = "checks/ReplaceLambdaByMethodRefCheck_no_version.java";

  @Test
  void java8() {
    InternalCheckVerifier.newInstance()
      .onFile(testSourcesPath(FILENAME))
      .withCheck(new ReplaceLambdaByMethodRefCheck())
      .withJavaVersion(8)
      .withQuickFixes()
      .verifyIssues();
    InternalCheckVerifier.newInstance()
      .onFile(testSourcesPath(FILENAME))
      .withCheck(new ReplaceLambdaByMethodRefCheck())
      .withJavaVersion(8)
      .withoutSemantic()
      .withQuickFixes()
      .verifyIssues();
  }

  @Test
  void nonCompiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath(FILENAME))
      .withCheck(new ReplaceLambdaByMethodRefCheck())
      .withJavaVersion(8)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath(FILENAME))
      .withCheck(new ReplaceLambdaByMethodRefCheck())
      .withJavaVersion(8)
      .withoutSemantic()
      .verifyIssues();
  }

  @Test
  void no_version() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath(NO_VERSION_FILENAME))
      .withCheck(new ReplaceLambdaByMethodRefCheck())
      .verifyIssues();
  }
}
