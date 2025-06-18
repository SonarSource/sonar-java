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

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class StringIsEmptyCheckTest {

  @Test
  void string() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/StringIsEmptyCheckSample.java"))
      .withCheck(new StringIsEmptyCheck())
      .verifyIssues();
  }

  @Test
  void string_15() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/StringIsEmptyCheckSample.java"))
      .withCheck(new StringIsEmptyCheck())
      .withJavaVersion(15)
      .verifyIssues();
  }

  @Test
  void charSequence_15() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/CharSequenceIsEmptyCheckSample.java"))
      .withCheck(new StringIsEmptyCheck())
      .withJavaVersion(15)
      .verifyIssues();
  }

  @Test
  void charSequence_8() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/CharSequenceIsEmptyCheckSample.java"))
      .withCheck(new StringIsEmptyCheck())
      .withJavaVersion(8)
      .verifyNoIssues();
  }

  @Test
  void testOlderJavaVersion() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/StringIsEmptyCheckSample.java"))
      .withCheck(new StringIsEmptyCheck())
      .withJavaVersion(5)
      .verifyNoIssues();
  }
}
