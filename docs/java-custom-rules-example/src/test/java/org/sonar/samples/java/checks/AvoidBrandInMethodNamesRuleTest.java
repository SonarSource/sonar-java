/*
 * Copyright (C) 2012-2022 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package org.sonar.samples.java.checks;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.checks.verifier.CheckVerifier;

@EnableRuleMigrationSupport
class AvoidBrandInMethodNamesRuleTest {

  // Set a LogTester to see the Syntax Tree when running tests and executing the rule
  @Rule
  public LogTester logTester = new LogTester().setLevel(LoggerLevel.DEBUG);

  @Test
  void detected() {
    // Verifies that the check will raise the adequate issues with the expected message.
    // In the test file, lines which should raise an issue have been commented out
    // by using the following syntax: "// Noncompliant {{EXPECTED_MESSAGE}}"
    CheckVerifier.newVerifier()
      .onFile("src/test/files/AvoidBrandInMethodNamesRule.java")
      .withCheck(new AvoidBrandInMethodNamesRule())
      .verifyIssues();
  }
}
