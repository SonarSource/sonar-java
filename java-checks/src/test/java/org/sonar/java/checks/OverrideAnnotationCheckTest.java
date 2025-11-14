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
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class OverrideAnnotationCheckTest {

  @Test
  void test_java() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/OverrideAnnotationCheckSample.java"))
      .withCheck(new OverrideAnnotationCheck())
      .verifyIssues();
  }

  @Test
  void quickfixes() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/OverrideAnnotationCheck_QuickFixes.java"))
      .withCheck(new OverrideAnnotationCheck())
      .verifyIssues();
  }

  @Test
  void test_java_8() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/OverrideAnnotationCheck_java8.java"))
      .withCheck(new OverrideAnnotationCheck())
      .withJavaVersion(8)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/OverrideAnnotationCheckSample.java"))
      .withCheck(new OverrideAnnotationCheck())
      .withJavaVersion(8)
      .verifyIssues();
  }

  @Test
  void test_java_6() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/OverrideAnnotationCheckSample.java"))
      .withCheck(new OverrideAnnotationCheck())
      .withJavaVersion(6)
      .verifyIssues();
  }

  @Test
  void test_java_5() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/OverrideAnnotationCheck_java5.java"))
      .withCheck(new OverrideAnnotationCheck())
      .withJavaVersion(5)
      .verifyIssues();
  }

  @Test
  void test_java_4() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/OverrideAnnotationCheck_java4.java"))
      .withCheck(new OverrideAnnotationCheck())
      .withJavaVersion(4)
      .verifyNoIssues();
  }

}
