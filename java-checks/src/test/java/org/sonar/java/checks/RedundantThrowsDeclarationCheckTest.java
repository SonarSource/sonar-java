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
package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class RedundantThrowsDeclarationCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/RedundantThrowsDeclarationCheckSample.java"))
      .withCheck(new RedundantThrowsDeclarationCheck())
      .verifyIssues();
  }

  @Test
  void test_non_compiling_code() {
    // a throws clause on a compact constructor is a syntax error: the file is reported as a parse error
    CheckVerifier verifier = CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/RedundantThrowsDeclarationCheckSample.java"))
      .withCheck(new RedundantThrowsDeclarationCheck());
    assertThatThrownBy(verifier::verifyIssues)
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("Parse error at line 55");
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/RedundantThrowsDeclarationCheckSample.java"))
      .withCheck(new RedundantThrowsDeclarationCheck())
      .withoutSemantic()
      .verifyIssues();
  }
}
