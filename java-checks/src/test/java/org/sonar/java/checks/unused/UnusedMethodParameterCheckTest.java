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
package org.sonar.java.checks.unused;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.plugins.java.api.JavaFileScanner;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class UnusedMethodParameterCheckTest {
  private static final JavaFileScanner CHECK = new UnusedMethodParameterCheck();

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/unused/UnusedMethodParameterCheckSample.java"))
      .withCheck(CHECK)
      .verifyIssues();
  }

  @Test
  void test_main_method_java21() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/unused/UnusedMethodParameterCheckMainSample.java"))
      .withCheck(CHECK)
      .withJavaVersion(21)
      .verifyNoIssues();
  }

  @Test
  void test_main_method_java25() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/unused/UnusedMethodParameterCheckMainSample.java"))
      .withCheck(CHECK)
      .withJavaVersion(25)
      .verifyIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/unused/UnusedMethodParameterCheckSample.java"))
      .withCheck(CHECK)
      .verifyIssues();
  }
}
