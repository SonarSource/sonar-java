/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

class MainMethodSignatureCheckTest {

  private static final MainMethodSignatureCheck CHECK = new MainMethodSignatureCheck();

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/mainSignature/Sample.java"))
      .withCheck(CHECK)
      .verifyIssues();
  }

  @Test
  void nonInstantiable() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/mainSignature/NonInstantiable.java"))
      .withCheck(CHECK)
      .verifyIssues();
  }

  @Test
  void compactSource() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/mainSignature/CompactSource.java"))
      .withCheck(CHECK)
      .verifyNoIssues();
  }
}
