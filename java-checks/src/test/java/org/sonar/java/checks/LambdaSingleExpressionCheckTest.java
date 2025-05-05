/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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

class LambdaSingleExpressionCheckTest {

  @Test
  void no_version() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/LambdaSingleExpressionCheck_no_version.java")
      .withCheck(new LambdaSingleExpressionCheck())
      .verifyIssues();
  }

  @Test
  void java_8() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/LambdaSingleExpressionCheck.java")
      .withCheck(new LambdaSingleExpressionCheck())
      .withJavaVersion(8)
      .verifyIssues();
  }

  @Test
  void ambiguous() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/LambdaSingleExpressionCheckSample.java"))
      .withCheck(new LambdaSingleExpressionCheck())
      .verifyIssues();
  }
}
