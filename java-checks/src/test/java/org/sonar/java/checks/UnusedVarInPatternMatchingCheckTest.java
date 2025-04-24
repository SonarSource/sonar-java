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
import org.sonar.plugins.java.api.JavaFileScanner;

class UnusedVarInPatternMatchingCheckTest {

  private static final String SAMPLE_FILE = TestUtils.mainCodeSourcesPath("checks/UnusedVarInPatternMatchingCheckSample.java");
  private static final JavaFileScanner CHECK = new UnusedVarInPatternMatchingCheck();

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(SAMPLE_FILE)
      .withCheck(CHECK)
      .withJavaVersion(22)
      .verifyIssues();
  }

  @Test
  void test_prior_to_java_22() {
    CheckVerifier.newVerifier()
      .onFile(SAMPLE_FILE)
      .withCheck(CHECK)
      .withJavaVersion(21)
      .verifyNoIssues();
  }

}
