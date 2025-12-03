/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class DiamondOperatorCheckTest {

  @Test
  void test_no_version() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/DiamondOperatorCheck_no_version.java"))
      .withCheck(new DiamondOperatorCheck())
      .verifyIssues();
  }

  @Test
  void test_with_java_7() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/DiamondOperatorCheck_java_7.java"))
      .withCheck(new DiamondOperatorCheck())
      .withJavaVersion(7)
      .verifyIssues();
  }

  @Test
  void test_with_java_8() {
    // take into account ternary operators
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/DiamondOperatorCheck_java_8.java"))
      .withCheck(new DiamondOperatorCheck())
      .withJavaVersion(8)
      .verifyIssues();
  }

}
