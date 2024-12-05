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
package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class StaticMemberAccessCheckTest {
  private static final String COMPILING_SAMPLE_FOLDER = "checks/S3252_StaticMemberAccessCheckSample/";

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(COMPILING_SAMPLE_FOLDER+"StaticMemberAccessCheckSample.java"))
      .withCheck(new StaticMemberAccessCheck())
      .verifyIssues();
  }

  @Test
  void test_default_package() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(COMPILING_SAMPLE_FOLDER+"S3252TestDefaultPackage.java"))
      .withCheck(new StaticMemberAccessCheck())
      .verifyIssues();
  }

  @Test
  void quick_fixes() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/StaticMemberAccessQuickFixes.java"))
      .withCheck(new StaticMemberAccessCheck())
      .verifyIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/StaticMemberAccessCheckSample.java"))
      .withCheck(new StaticMemberAccessCheck())
      .verifyIssues();
  }

}
