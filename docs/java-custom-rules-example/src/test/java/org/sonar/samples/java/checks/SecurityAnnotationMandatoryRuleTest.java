/*
 * Copyright (C) 2012-2023 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package org.sonar.samples.java.checks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.checks.verifier.CheckVerifier;

class SecurityAnnotationMandatoryRuleTest {

  // Set a LogTester to see the Syntax Tree when running tests and executing the rule
  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

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
