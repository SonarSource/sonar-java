/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.samples.java.checks;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.checks.verifier.CheckVerifier;

@EnableRuleMigrationSupport
class SecurityAnnotationMandatoryRuleTest {

  // Set a LogTester to see the Syntax Tree when running tests and executing the rule
  @Rule
  public LogTester logTester = new LogTester().setLevel(LoggerLevel.DEBUG);

  @Test
  void detected() {
    // Use an instance of the check under test to raise the issue.
    SecurityAnnotationMandatoryRule check = new SecurityAnnotationMandatoryRule();

    // define the mandatory annotation name
    check.name = "MySecurityAnnotation";

    // Verifies that the check will raise the adequate issues with the expected message.
    // In the test file, lines which should raise an issue have been commented out
    // by using the following syntax: "// Noncompliant {{EXPECTED_MESSAGE}}"
    CheckVerifier.newVerifier().onFile("src/test/files/SecurityAnnotationMandatoryRule.java").withCheck(check).verifyIssues();
  }

  @Test
  void without_package_name() {
    // Use an instance of the check under test to raise the issue.
    SecurityAnnotationMandatoryRule check = new SecurityAnnotationMandatoryRule();

    // define the mandatory annotation name
    check.name = "MySecurityAnnotation";

    // Verifies that the check will raise the adequate issues with the expected message.
    // In the test file, lines which should raise an issue have been commented out
    // by using the following syntax: "// Noncompliant {{EXPECTED_MESSAGE}}"
    CheckVerifier.newVerifier().onFile("src/test/files/SecurityAnnotationMandatoryRuleWithoutPackage.java").withCheck(check).verifyIssues();
  }

}
