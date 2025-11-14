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

class MismatchPackageDirectoryCheckTest {

  @Test
  void correctMatch() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/mismatchPackage/Matching.java"))
      .withCheck(new MismatchPackageDirectoryCheck())
      .verifyNoIssues();
  }

  @Test
  void defaultPackage() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/mismatchPackage/DefaultPackageCase.java"))
      .withCheck(new MismatchPackageDirectoryCheck())
      .verifyNoIssues();
  }

  @Test
  void mismatch() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/mismatchPackage/Mismatch.java"))
      .withCheck(new MismatchPackageDirectoryCheck())
      .verifyIssues();
  }

  @Test
  void mismatch_with_root_directory() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/mismatchPackage/Mismatch.java"))
      .withCheck(new MismatchPackageDirectoryCheck())
      .withProjectLevelWorkDir("src")
      .verifyIssues();
  }

  @Test
  void mismatch_with_wrong_root_directory() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/mismatchPackage/Mismatch.java"))
      .withCheck(new MismatchPackageDirectoryCheck())
      .withProjectLevelWorkDir("notADirectory")
      .verifyIssues();
  }

  @Test
  void mismatchWithDots() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/mismatchPackage/with.dots/PackageWithDots.java"))
      .withCheck(new MismatchPackageDirectoryCheck())
      .verifyIssues();
  }

}
