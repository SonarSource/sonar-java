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

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class CastArithmeticOperandCheckTest {

  private static final String TEST_FILE_PATH = "checks/CastArithmeticOperandCheck.java";

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(TEST_FILE_PATH))
      .withCheck(new CastArithmeticOperandCheck())
      .verifyIssues();

    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath(TEST_FILE_PATH))
      .withCheck(new CastArithmeticOperandCheck())
      .verifyIssues();
  }

  @Test
  void test_no_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(TEST_FILE_PATH))
      .withCheck(new CastArithmeticOperandCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }
}
