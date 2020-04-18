/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import static org.sonar.java.CheckTestUtils.testSourcesPath;

public class CallToDeprecatedMethodCheckTest {

  @Test
  public void test() {
    JavaCheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/CallToDeprecatedMethod.java"))
      .withCheck(new CallToDeprecatedMethodCheck())
      .verifyIssues();
  }

  /**
   * See {@link CallToDeprecatedCodeMarkedForRemovalCheck}
   */
  @Test
  public void flagged_for_removal_should_not_raise_issue() {
    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/checks/CallToDeprecatedMethod_java9.java")
      .withJavaVersion(9)
      .withCheck(new CallToDeprecatedMethodCheck())
      .verifyIssues();
  }

  @Test
  public void without_semantic() {
    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/checks/CallToDeprecatedMethod_noSemantic.java")
      .withCheck(new CallToDeprecatedMethodCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }
}
