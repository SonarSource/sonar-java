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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReplaceUnusedExceptionParameterWithUnnamedPatternCheckTest {

  @Test
  void test_java22() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/ReplaceUnusedExceptionParameterWithUnnamedPatternCheckSample.java"))
      .withCheck(new ReplaceUnusedExceptionParameterWithUnnamedPatternCheck())
      .withJavaVersion(22)
      .verifyIssues();
  }

  @Test
  void test_java21() {

    assertThatThrownBy(() -> {
      CheckVerifier.newVerifier()
        .onFile(TestUtils.mainCodeSourcesPath("checks/ReplaceUnusedExceptionParameterWithUnnamedPatternCheckSample.java"))
        .withCheck(new ReplaceUnusedExceptionParameterWithUnnamedPatternCheck())
        .withJavaVersion(21)
        .verifyNoIssues();
    })
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("'_' is a keyword from source level 9 onwards, cannot be used as identifier");

  }

  @Test
  void test_without_semantics() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/ReplaceUnusedExceptionParameterWithUnnamedPatternCheckSample.java"))
      .withCheck(new ReplaceUnusedExceptionParameterWithUnnamedPatternCheck())
      .withoutSemantic()
      .withJavaVersion(22)
      .verifyIssues();
  }

}
