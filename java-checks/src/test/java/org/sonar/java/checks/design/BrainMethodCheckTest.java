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

package org.sonar.java.checks.design;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.internal.InternalCheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class BrainMethodCheckTest {

  private static final String highComplexityFilePath = mainCodeSourcesPath("checks/BrainMethodCheck.java");
  private static final String lowComplexityFilePath = mainCodeSourcesPath("checks/BrainMethodCheckLowerThresholds.java");
  private static final String subsetFilePath = mainCodeSourcesPath("checks/BrainMethodCheckSubsetOfIssues.java");

  @Test
  void testHighComplexityFileWithDefaultThresholds() {
    InternalCheckVerifier.newInstance()
      .onFile(highComplexityFilePath)
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

    InternalCheckVerifier.newInstance()
      .onFile(highComplexityFilePath)
      .withChecks(check)
      .verifyNoIssues();
  }

  @Test
  void testLowComplexityFileWithDefaultThresholds() {
    InternalCheckVerifier.newInstance()
      .onFile(lowComplexityFilePath)
      .withChecks(new BrainMethodCheck())
      .verifyNoIssues();
  }

  @Test
  void testLowComplexityFileWithLowerThresholds() {
    var check = new BrainMethodCheck();

    check.locThreshold = 14;
    check.noavThreshold = 4;
    check.cyclomaticThreshold = 5;

    InternalCheckVerifier.newInstance()
      .onFile(lowComplexityFilePath)
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
    
    InternalCheckVerifier.newInstance()
      .onFile(subsetFilePath)
      .withChecks(check)
      .verifyIssues();
  }

}
