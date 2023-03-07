/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.internal.InternalCheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class UselessImportCheckTest {

  @Test
  void detected_with_package() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/UselessImportCheck/WithinPackage.java"))
      .withCheck(new UselessImportCheck())
      .verifyIssues();
  }

  @Test
  void quickFixes() {
    InternalCheckVerifier.newInstance()
      .onFile(testSourcesPath("checks/UselessImportCheck/WithQuickFixes.java"))
      .withCheck(new UselessImportCheck())
      .withQuickFixes()
      .verifyIssues();

    InternalCheckVerifier.newInstance()
      .onFile(testSourcesPath("checks/UselessImportCheck/WithQuickFixesSingleImport.java"))
      .withCheck(new UselessImportCheck())
      .withQuickFixes()
      .verifyIssues();
  }

  @Test
  void records() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/UselessImportCheck/records.java"))
      .withCheck(new UselessImportCheck())
      .verifyIssues();
  }

  @Test
  void detected_within_package_info() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/UselessImportCheck/package-info.java"))
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
      .onFile(testSourcesPath("WithoutPackage.java"))
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
      .onFile(testSourcesPath("checks/UselessImportCheck/IntersectionCase.java"))
      .withCheck(new UselessImportCheck())
      .verifyIssues();
  }

}
