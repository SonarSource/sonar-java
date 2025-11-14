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

class MagicNumberCheckTest {

  @Test
  void detected() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/MagicNumberCheck.java")
      .withCheck(new MagicNumberCheck())
      .verifyIssues();
  }

  @Test
  void detectedWithTwoAuthorized() {
    MagicNumberCheck check = new MagicNumberCheck();
    check.authorizedNumbers = "-1,0,1,2";
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/MagicNumberCheckCustom.java")
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void detectedWithAuthorizedNumberSpaces() {
    MagicNumberCheck check = new MagicNumberCheck();
    check.authorizedNumbers = " -1,0 , 1 ,2";
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/MagicNumberCheckCustom.java")
      .withCheck(check)
      .verifyIssues();
  }
}
