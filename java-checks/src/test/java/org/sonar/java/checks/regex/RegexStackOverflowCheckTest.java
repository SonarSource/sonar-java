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
package org.sonar.java.checks.regex;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class RegexStackOverflowCheckTest {

  @Test
  void testWithDefaultMax() {
    RegexStackOverflowCheck check = new RegexStackOverflowCheck();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithHighStackConsumption.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithMediumStackConsumption.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithLowStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithConstantStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void testWithZeroMax() {
    RegexStackOverflowCheck check = new RegexStackOverflowCheck();
    check.setMaxStackConsumptionFactor(0);
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithHighStackConsumption.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithMediumStackConsumption.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithLowStackConsumption.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithConstantStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void testWithHigherMax() {
    RegexStackOverflowCheck check = new RegexStackOverflowCheck();
    check.setMaxStackConsumptionFactor(7);
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithHighStackConsumption.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithMediumStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithLowStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithConstantStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void testWithEvenHigherMax() {
    RegexStackOverflowCheck check = new RegexStackOverflowCheck();
    check.setMaxStackConsumptionFactor(20);
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithHighStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithMediumStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithLowStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/RegexStackOverflowCheckWithConstantStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

}
