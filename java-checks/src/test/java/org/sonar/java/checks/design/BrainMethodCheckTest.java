/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
  private static final String SUBSET_SMALL_FILE_PATH = mainCodeSourcesPath("checks/BrainMethodCheckSubsetSmall.java");
  private static final String SUBSET_FILE_PATH = mainCodeSourcesPath("checks/BrainMethodCheckSubsetOfIssues.java");
  private static final String SUBSET_LARGE_FILE_PATH = mainCodeSourcesPath("checks/BrainMethodCheckSubsetLarge.java");
  private static final String SUBSET_CAPPED_FILE_PATH = mainCodeSourcesPath("checks/BrainMethodCheckSubsetCapped.java");

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
    check.nodvThreshold = 36;
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
    check.nodvThreshold = 4;
    check.cyclomaticThreshold = 5;

    CheckVerifier.newVerifier()
      .onFile(LOW_COMPLEXITY_FILE_PATH)
      .withChecks(check)
      .verifyIssues();
  }

  @Test
  void testSubsetAllIssuesReportedBelowThreshold() {
    // 3 issues found < numberOfFoundIssuesThreshold=5: all issues are reported
    CheckVerifier.newVerifier()
      .onFile(SUBSET_SMALL_FILE_PATH)
      .withChecks(checkForSubsetTests())
      .verifyIssues();
  }

  @Test
  void testSubsetPercentageAppliedBetweenThresholds() {
    // 10 issues found, numberOfFoundIssuesThreshold(5) < 10 < numberOfIssuesPerModuleThreshold(20):
    // reports min(10 * 10 / 100, 20) = 1 issue
    CheckVerifier.newVerifier()
      .onFile(SUBSET_FILE_PATH)
      .withChecks(checkForSubsetTests())
      .verifyIssues();
  }

  @Test
  void testSubsetPercentageAppliedAboveModuleThreshold() {
    // 21 issues found > numberOfIssuesPerModuleThreshold=20:
    // reports min(21 * 10 / 100, 20) = 2 issues
    CheckVerifier.newVerifier()
      .onFile(SUBSET_LARGE_FILE_PATH)
      .withChecks(checkForSubsetTests())
      .verifyIssues();
  }

  @Test
  void testSubsetCappedAtModuleThreshold() {
    // 60 issues found, 10% = 6 > numberOfIssuesPerModuleThreshold=5:
    // reports min(60 * 10 / 100, 5) = 5 issues — cap is the binding constraint
    CheckVerifier.newVerifier()
      .onFile(SUBSET_CAPPED_FILE_PATH)
      .withChecks(checkForCappedSubsetTest())
      .verifyIssues();
  }

  private static BrainMethodCheck checkForSubsetTests() {
    var check = new BrainMethodCheck();
    check.locThreshold = 4;
    check.nodvThreshold = 2;
    check.cyclomaticThreshold = 1;
    check.nestingThreshold = 1;
    check.numberOfFoundIssuesThreshold = 5;
    check.issuesToReportPercentage = 10;
    check.numberOfIssuesPerModuleThreshold = 20;
    return check;
  }

  private static BrainMethodCheck checkForCappedSubsetTest() {
    var check = new BrainMethodCheck();
    check.locThreshold = 4;
    check.nodvThreshold = 2;
    check.cyclomaticThreshold = 1;
    check.nestingThreshold = 1;
    check.numberOfFoundIssuesThreshold = 5;
    check.issuesToReportPercentage = 10;
    check.numberOfIssuesPerModuleThreshold = 5;
    return check;
  }

}
