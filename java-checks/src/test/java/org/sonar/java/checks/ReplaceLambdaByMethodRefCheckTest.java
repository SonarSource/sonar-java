/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class ReplaceLambdaByMethodRefCheckTest {

  private static final String FILENAME = "checks/ReplaceLambdaByMethodRefCheckSample.java";
  public static final String NO_VERSION_FILENAME = "checks/ReplaceLambdaByMethodRefCheck_no_version.java";

  @Test
  void java8() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(FILENAME))
      .withCheck(new ReplaceLambdaByMethodRefCheck())
      .withJavaVersion(8)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(FILENAME))
      .withCheck(new ReplaceLambdaByMethodRefCheck())
      .withJavaVersion(8)
      .withoutSemantic()
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
      .onFile(mainCodeSourcesPath(NO_VERSION_FILENAME))
      .withCheck(new ReplaceLambdaByMethodRefCheck())
      .verifyIssues();
  }
}
