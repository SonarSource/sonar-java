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

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class NestedIfStatementsCheckTest {

  @Test
  void detected() {
    NestedIfStatementsCheck check = new NestedIfStatementsCheck();
    assertThat(check.max).isEqualTo(3);
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/NestedIfStatementsCheckSample.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void custom() {
    NestedIfStatementsCheck check = new NestedIfStatementsCheck();
    check.max = 4;
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/NestedIfStatementsCheckCustom.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void detected_switch_expressions() {
    NestedIfStatementsCheck check = new NestedIfStatementsCheck();
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/NestedIfStatementsCheckSample.java"))
      .withJavaVersion(14)
      .withCheck(check)
      .verifyIssues();
  }

}
