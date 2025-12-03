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

class UselessImportCheckTest {

  @Test
  void detected_with_package() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/UselessImportCheck/WithinPackage.java"))
      .withCheck(new UselessImportCheck())
      .verifyIssues();
  }

  @Test
  void quickFixes() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/UselessImportCheck/WithQuickFixes.java"))
      .withCheck(new UselessImportCheck())
      .verifyIssues();

    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/UselessImportCheck/WithQuickFixesSingleImport.java"))
      .withCheck(new UselessImportCheck())
      .verifyIssues();
  }

  @Test
  void records() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/UselessImportCheck/records.java"))
      .withCheck(new UselessImportCheck())
      .verifyIssues();
  }

  @Test
  void detected_within_package_info() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/UselessImportCheck/package-info.java"))
      .withCheck(new UselessImportCheck())
      .verifyIssues();
  }

  @Test
  void no_semantic() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/UselessImportCheck/NoSemanticWithPackage.java"))
      .withCheck(new UselessImportCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void detected_without_package() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("WithoutPackage.java"))
      .withCheck(new UselessImportCheck())
      .verifyIssues();
  }

  @Test
  void with_module() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("module/module-info.java"))
      .withCheck(new UselessImportCheck())
      .verifyNoIssues();
  }

  @Test
  void with_compiling_module() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("module-info.java"))
      .withCheck(new UselessImportCheck())
      .verifyNoIssues();
  }

  @Test
  void intersection_type() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/UselessImportCheck/IntersectionCase.java"))
      .withCheck(new UselessImportCheck())
      .verifyIssues();
  }

}
