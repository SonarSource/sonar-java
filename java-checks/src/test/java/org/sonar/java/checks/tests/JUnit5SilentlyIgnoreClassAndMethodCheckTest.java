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
package org.sonar.java.checks.tests;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testCodeSourcesPath;

class JUnit5SilentlyIgnoreClassAndMethodCheckTest {

  private static final String SOURCE_PATH = "checks/tests/JUnit5SilentlyIgnoreClassAndMethodCheckSample.java";

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath(SOURCE_PATH))
      .withCheck(new JUnit5SilentlyIgnoreClassAndMethodCheck())
      .verifyIssues();
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath(SOURCE_PATH))
      .withCheck(new JUnit5SilentlyIgnoreClassAndMethodCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void test_unknown_symbols() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath(SOURCE_PATH))
      .withCheck(new JUnit5SilentlyIgnoreClassAndMethodCheck())
      .verifyIssues();
  }

}
