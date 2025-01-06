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
import org.sonar.java.checks.verifier.TestUtils;

class ReturnEmptyArrayNotNullCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/ReturnEmptyArrayNotNullCheckSample.java"))
      .withCheck(new ReturnEmptyArrayNotNullCheck())
      .verifyIssues();
  }

  @Test
  void quick_fixes() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/ReturnEmptyArrayNotNullCheckWithQuickFixes.java"))
      .withCheck(new ReturnEmptyArrayNotNullCheck())
      .verifyIssues();

    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/ReturnEmptyArrayNotNullCheckWithQuickFixesAndImports.java"))
      .withCheck(new ReturnEmptyArrayNotNullCheck())
      .verifyIssues();
  }

  @Test
  void test_jspecify_nullmarked() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/jspecify/ReturnEmptyArrayNotNullCheckNullMarked.java"))
      .withCheck(new ReturnEmptyArrayNotNullCheck())
      .verifyIssues();

    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/jspecify/nullmarked/ReturnEmptyArrayNotNullCheck.java"))
      .withCheck(new ReturnEmptyArrayNotNullCheck())
      .verifyIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/ReturnEmptyArrayNotNullCheckSample.java"))
      .withCheck(new ReturnEmptyArrayNotNullCheck())
      .verifyIssues();
  }

}
