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

class ThreadLocalWithInitialCheckTest {
  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/ThreadLocalWithInitialCheck.java")
      .withCheck(new ThreadLocalWithInitialCheck())
      .verifyIssues();
  }

  @Test
  void no_issue_prior_to_java_8() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/ThreadLocalWithInitialCheck_java7.java"))
      .withCheck(new ThreadLocalWithInitialCheck())
      .withJavaVersion(7)
      .verifyNoIssues();
  }
}
