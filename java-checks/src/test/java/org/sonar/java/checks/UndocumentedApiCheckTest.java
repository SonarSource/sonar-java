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

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class UndocumentedApiCheckTest {

  @Test
  void test() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    assertThat(check.forClasses).isEqualTo("**.api.**");
    assertThat(check.exclusion).isEqualTo("**.internal.**");
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/UndocumentedApiCheck/UndocumentedApi.java")
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void java_16() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    assertThat(check.forClasses).isEqualTo("**.api.**");
    assertThat(check.exclusion).isEqualTo("**.internal.**");
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/api/undocumentedAPI/UndocumentedAPI_java16.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void testMissingConfiguration() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    check.forClasses = null;
    check.exclusion = null;
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/UndocumentedApiCheck/UndocumentedApi.java")
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void no_issue_without_Semantic() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/UndocumentedApiCheck/UndocumentedApi.java")
      .withCheck(check)
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void custom() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    check.forClasses = "**.open.**";
    check.exclusion = "";
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/UndocumentedApiCheck/UndocumentedApiCustom.java")
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void testExclusion() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    check.forClasses = "";
    check.exclusion = "**.internal.**";
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/UndocumentedApiCheck/UndocumentedApiExclusion.java")
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void testIncompleteJavadoc() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    check.forClasses = "";
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/UndocumentedApiCheck/UndocumentedApiIncomplete.java")
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void testInvalidDeclaredException() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    check.forClasses = "";
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/UndocumentedApiCheck/UndocumentedApiInvalidException.java")
      .withCheck(check)
      .verifyNoIssues();
  }
}
