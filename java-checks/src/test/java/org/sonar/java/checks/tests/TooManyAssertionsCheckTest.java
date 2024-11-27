/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.tests;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testCodeSourcesPath;

class TooManyAssertionsCheckTest {
  @Test
  void detected() {
    assertThat(new TooManyAssertionsCheck().maximum).isEqualTo(25);
  }

  @Test
  void custom_at_2() {
    TooManyAssertionsCheck check = new TooManyAssertionsCheck();
    check.maximum = 2;
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/TooManyAssertionsCheckCustom2.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void custom_at_25() {
    TooManyAssertionsCheck check = new TooManyAssertionsCheck();
    check.maximum = 25;
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/TooManyAssertionsCheckCustom25.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void nonCompiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/tests/TooManyAssertionsCheckSample.java"))
      .withCheck(new TooManyAssertionsCheck())
      .verifyNoIssues();
  }

}
