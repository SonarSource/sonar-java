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
package org.sonar.java.checks.regex;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class RegexStackOverflowCheckTest {

  @Test
  void testWithDefaultMax() {
    RegexStackOverflowCheck check = new RegexStackOverflowCheck();
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithHighStackConsumption.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithMediumStackConsumption.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithLowStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithConstantStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void testWithZeroMax() {
    RegexStackOverflowCheck check = new RegexStackOverflowCheck();
    check.setMaxStackConsumptionFactor(0);
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithHighStackConsumption.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithMediumStackConsumption.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithLowStackConsumption.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithConstantStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void testWithHigherMax() {
    RegexStackOverflowCheck check = new RegexStackOverflowCheck();
    check.setMaxStackConsumptionFactor(7);
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithHighStackConsumption.java"))
      .withCheck(check)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithMediumStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithLowStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithConstantStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void testWithEvenHigherMax() {
    RegexStackOverflowCheck check = new RegexStackOverflowCheck();
    check.setMaxStackConsumptionFactor(20);
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithHighStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithMediumStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithLowStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/RegexStackOverflowCheckWithConstantStackConsumption.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

}
