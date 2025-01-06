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
package org.sonar.java.checks.naming;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class BadMethodNameCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/BadMethodName.java"))
      .withCheck(new BadMethodNameCheck())
      .verifyIssues();
  }

  @Test
  void testWithoutSemantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/BadMethodName.java"))
      .withCheck(new BadMethodNameCheck())
      .withoutSemantic()
      .verifyIssues();
  }

  @Test
  void testWithCustomNameFormat() {
    BadMethodNameCheck check = new BadMethodNameCheck();
    check.format = "^[a-zA-Z0-9]*$";
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/BadMethodNameCustom.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void testOverrideWithoutAnnotation() {
    BadMethodNameCheck check = new BadMethodNameCheck();
    check.format = "^[A-Z0-9]*$";
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/BadMethodNameCustomNoncompliant.java"))
      .withCheck(check)
      .verifyIssues();
  }
}
