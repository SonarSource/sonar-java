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
package org.sonar.java.checks.naming;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class BadLocalVariableNameCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/BadLocalVariableNameCheckSample.java"))
      .withCheck(new BadLocalVariableNameCheck())
      .verifyIssues();
  }

  @Test
  void test2() {
    BadLocalVariableNameCheck check = new BadLocalVariableNameCheck();
    check.format = "^[a-zA-Z0-9_][a-zA-Z0-9_][a-zA-Z0-9_][a-zA-Z0-9_]*$";
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/BadLocalVariableNameCheckSample_Custom.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

}
