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

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class NullShouldNotBeUsedWithOptionalCheckTest {

  @Test
  void test() {
    NullShouldNotBeUsedWithOptionalCheck check = new NullShouldNotBeUsedWithOptionalCheck();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/NullShouldNotBeUsedWithOptionalCheck_jdk.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/NullShouldNotBeUsedWithOptionalCheck_guava.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void test_jspecify_null_marked() {
    NullShouldNotBeUsedWithOptionalCheck check = new NullShouldNotBeUsedWithOptionalCheck();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/jspecify/NullShouldNotBeUsedWithOptionalCheckNullMarked_jdk.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/jspecify/NullShouldNotBeUsedWithOptionalCheckNullMarked_guava.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/jspecify/nullmarked/NullShouldNotBeUsedWithOptionalCheck_jdk.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/jspecify/nullmarked/NullShouldNotBeUsedWithOptionalCheck_guava.java"))
      .withCheck(check)
      .verifyIssues();
  }

}
