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
package org.sonar.java.checks.design;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class BrainMethodCheckTest {

  private static final String HIGH_COMPLEXITY_FILE_PATH = mainCodeSourcesPath("checks/BrainMethodCheckSample.java");
  private static final String LOW_COMPLEXITY_FILE_PATH = mainCodeSourcesPath("checks/BrainMethodCheckLowerThresholds.java");
  private static final String SUBSET_FILE_PATH = mainCodeSourcesPath("checks/BrainMethodCheckSubsetOfIssues.java");

  @Test
  void testHighComplexityFileWithDefaultThresholds() {
    CheckVerifier.newVerifier()
      .onFile(HIGH_COMPLEXITY_FILE_PATH)
      .withChecks(new BrainMethodCheck())
      .verifyIssues();
  }

  @Test
  void testHighComplexityFileWithHigherThresholds() {
    var check = new BrainMethodCheck();

    check.locThreshold = 120;
    check.noavThreshold = 36;
    check.nestingThreshold = 8;
    check.cyclomaticThreshold = 45;

    CheckVerifier.newVerifier()
      .onFile(HIGH_COMPLEXITY_FILE_PATH)
      .withChecks(check)
      .verifyNoIssues();
  }

  @Test
  void testLowComplexityFileWithDefaultThresholds() {
    CheckVerifier.newVerifier()
      .onFile(LOW_COMPLEXITY_FILE_PATH)
      .withChecks(new BrainMethodCheck())
      .verifyNoIssues();
  }

  @Test
  void testLowComplexityFileWithLowerThresholds() {
    var check = new BrainMethodCheck();

    check.locThreshold = 14;
    check.noavThreshold = 4;
    check.cyclomaticThreshold = 5;

    CheckVerifier.newVerifier()
      .onFile(LOW_COMPLEXITY_FILE_PATH)
      .withChecks(check)
      .verifyIssues();
  }

  @Test
  void testSubsetOfIssuesWithLowerThresholds() {
    var check = new BrainMethodCheck();

    check.locThreshold = 4;
    check.noavThreshold = 2;
    check.cyclomaticThreshold = 1;
    check.nestingThreshold = 1;

    check.numberOfIssuesToReport = 1;

    CheckVerifier.newVerifier()
      .onFile(SUBSET_FILE_PATH)
      .withChecks(check)
      .verifyIssues();
  }

}
