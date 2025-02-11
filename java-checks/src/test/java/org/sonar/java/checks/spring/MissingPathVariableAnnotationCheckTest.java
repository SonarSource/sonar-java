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
package org.sonar.java.checks.spring;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.plugins.java.api.JavaFileScanner;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class MissingPathVariableAnnotationCheckTest {
  private static final String TEST_SOURCE_FILE = mainCodeSourcesPath("checks/spring/MissingPathVariableAnnotationCheckSample.java");
  private static final JavaFileScanner check = new MissingPathVariableAnnotationCheck();

  @Test
  void test_compiling() {
    CheckVerifier.newVerifier()
      .onFile(TEST_SOURCE_FILE)
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void test_without_semantic(){
    CheckVerifier.newVerifier()
      .onFile(TEST_SOURCE_FILE)
      .withCheck(check)
      .withoutSemantic()
      .verifyNoIssues();
  }
  
}
