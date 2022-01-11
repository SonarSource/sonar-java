/*
 * Copyright (C) 2012-2022 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package org.sonar.samples.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

class AvoidAnnotationRuleTest {

  @Test
  void detected() {

    // Use an instance of the check under test to raise the issue.
    AvoidAnnotationRule rule = new AvoidAnnotationRule();

    // define the forbidden annotation name
    rule.name = "Zuper";

    // Verifies that the check will raise the adequate issues with the expected message.
    // In the test file, lines which should raise an issue have been commented out
    // by using the following syntax: "// Noncompliant {{EXPECTED_MESSAGE}}"
    CheckVerifier.newVerifier()
      .onFile("src/test/files/AvoidAnnotationRule.java")
      .withCheck(rule)
      .verifyIssues();
  }
}
