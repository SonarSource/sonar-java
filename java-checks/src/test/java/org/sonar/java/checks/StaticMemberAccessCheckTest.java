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
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class StaticMemberAccessCheckTest {
  private static final String COMPILING_SAMPLE_FOLDER = "checks/S3252_StaticMemberAccessCheckSample/";

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(COMPILING_SAMPLE_FOLDER + "StaticMemberAccessCheckSample.java"))
      .withCheck(new StaticMemberAccessCheck())
      .verifyIssues();
  }

  @Test
  void test_import_from_other_package() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(COMPILING_SAMPLE_FOLDER + "ImportFromOtherPackage.java"))
      .withCheck(new StaticMemberAccessCheck())
      .verifyIssues();
  }

  @Test
  void test_import_from_other_package_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(COMPILING_SAMPLE_FOLDER + "ImportFromOtherPackage.java"))
      .withoutSemantic()
      .withCheck(new StaticMemberAccessCheck())
      .verifyNoIssues();
  }

  @Test
  void test_when_class_is_in_default_package() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(COMPILING_SAMPLE_FOLDER + "ClassInDefaultPackage.java"))
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
