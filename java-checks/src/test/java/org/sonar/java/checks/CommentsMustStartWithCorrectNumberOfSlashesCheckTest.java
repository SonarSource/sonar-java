/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.plugins.java.api.JavaFileScanner;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class CommentsMustStartWithCorrectNumberOfSlashesCheckTest {
  private static final JavaFileScanner check = new CommentsMustStartWithCorrectNumberOfSlashesCheck();

  @Test
  void test_before_java17() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/CommentsMustStartWithCorrectNumberOfSlashesCheckSample.java"))
      .withCheck(check)
      .withJavaVersion(16)
      .verifyNoIssues();
  }

  @Test
  void test_before_java23() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/CommentsMustStartWithCorrectNumberOfSlashesCheckSample.java"))
      .withCheck(check)
      .withJavaVersion(22)
      .verifyIssues();
  }

  @Test
  void test_java23() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/CommentsMustStartWithCorrectNumberOfSlashesCheckJava23.java"))
      .withCheck(check)
      .withJavaVersion(23)
      .verifyIssues();
  }

}
