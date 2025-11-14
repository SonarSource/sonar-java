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
package org.sonar.java.checks.naming;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.testCodeSourcesPath;

class BadTestClassNameCheckTest {
  private static final String TEST_FILE = testCodeSourcesPath("checks/tests/BadTestClassNameCheckSample.java");

  @Test
  void test() {
    BadTestClassNameCheck check = new BadTestClassNameCheck();
    CheckVerifier.newVerifier()
      .onFile(TEST_FILE)
      .withCheck(check)
      .verifyIssues();
    // test with same instance to cover reuse of regexp pattern.
    CheckVerifier.newVerifier()
      .onFile(TEST_FILE)
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void test_no_semantic() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/BadTestClassNameCheckNoSemantic.java"))
      .withCheck(new BadTestClassNameCheck())
      .withoutSemantic()
      .verifyIssues();
  }

  @Test
  void test_with_customPattern() {
    BadTestClassNameCheck check = new BadTestClassNameCheck();
    check.format = "^[A-Z][a-zA-Z0-9]*SonarTest$";
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/BadTestClassNameCheckCustom.java"))
      .withCheck(check)
      .verifyIssues();
  }
}
