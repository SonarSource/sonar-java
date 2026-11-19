/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.security;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class UnpredictableSaltCheckTest {
  
  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/security/UnpredictableSaltCheck.java"))
      .withCheck(new UnpredictableSaltCheck())
      .verifyIssues();
  }

  @Test
  void test_non_compiling() {
    // an array creation without dimension nor initializer is a syntax error: the file is reported as a parse error
    CheckVerifier verifier = CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/security/UnpredictableSaltCheck.java"))
      .withCheck(new UnpredictableSaltCheck());
    assertThatThrownBy(verifier::verifyIssues)
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("Parse error at line 18");
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/security/UnpredictableSaltCheck.java"))
      .withCheck(new UnpredictableSaltCheck())
      .withoutSemantic()
      .verifyIssues();
  }
}
