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
import org.sonar.java.checks.verifier.TestUtils;

class SunPackagesUsedCheckTest {

  @Test
  void detected() {
    // Non-compiling code: without semantic information, we prefer to avoid false positives
    // even if it means missing some true positives. This prevents annoying FPs in AutoScan scenarios.
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/SunPackagesUsedCheckSample.java"))
      .withCheck(new SunPackagesUsedCheck())
      .verifyNoIssues();
  }

  @Test
  void check_with_exclusion() {
    // Non-compiling code: without semantic information, we prefer to avoid false positives
    SunPackagesUsedCheck check = new SunPackagesUsedCheck();
    check.exclude = "sun.excluded";
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/SunPackagesUsedCheckCustom.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void detected_with_semantic() {
    // With semantic information (compiling code with bytecode), we correctly distinguish
    // between variables named "sun" (compliant) and actual sun.* package usage (noncompliant).
    // This test file only contains variables named "sun", so no issues should be raised.
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/SunPackagesUsedCheckSample.java"))
      .withCheck(new SunPackagesUsedCheck())
      .verifyNoIssues();
  }

}
