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

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class ChangeMethodContractCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/S2638_ChangeMethodContractCheck/noPackageInfo/ChangeMethodContractCheck.java"))
      .withCheck(new ChangeMethodContractCheck())
      .verifyIssues();
  }

  @Test
  void test_package_level_annotations() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/S2638_ChangeMethodContractCheck/nonNullApi/ChangeMethodContractCheck.java"))
      .withCheck(new ChangeMethodContractCheck())
      .verifyIssues();
  }

  @Test
  void test_package_level_annotations_nullable_api() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/S2638_ChangeMethodContractCheck/nullableApi/ChangeMethodContractCheck.java"))
      .withCheck(new ChangeMethodContractCheck())
      .verifyIssues();
  }

  @Test
  void test_jspecify_null_marked() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/jspecify/ChangeMethodContractCheckNullMarked.java"))
      .withCheck(new ChangeMethodContractCheck())
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/jspecify/nullmarked/ChangeMethodContractCheck.java"))
      .withCheck(new ChangeMethodContractCheck())
      .verifyIssues();
  }

  @Test
  void non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/ChangeMethodContractCheck.java"))
      .withCheck(new ChangeMethodContractCheck())
      .verifyNoIssues();
  }

}
