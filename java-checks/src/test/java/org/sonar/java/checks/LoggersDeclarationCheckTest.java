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

class LoggersDeclarationCheckTest {

  @Test
  void detected() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/LoggersDeclarationCheck.java")
      .withCheck(new LoggersDeclarationCheck())
      .verifyIssues();
  }

  @Test
  void custom() {
    LoggersDeclarationCheck check = new LoggersDeclarationCheck();
    check.format = ".*";
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/LoggersDeclarationCheckCustom.java")
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void test_interface() {
    LoggersDeclarationCheck check = new LoggersDeclarationCheck();
    check.format = ".*";
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/LoggersDeclarationCheckInterface.java")
      .withCheck(check)
      .verifyNoIssues();
  }

}
