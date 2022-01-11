/*
 * Copyright (C) 2012-2022 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package org.sonar.samples.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.samples.java.utils.FilesUtils;

class AvoidSuperClassRuleTest {

  @Test
  void checkWithJarDependenciesInClassPath() throws Exception {
    // Verifies that the check will raise the adequate issues with the expected message.
    // In the test file, lines which should raise an issue have been commented out
    // by using the following syntax: "// Noncompliant {{EXPECTED_MESSAGE}}"
    CheckVerifier.newVerifier()
      .onFile("src/test/files/AvoidSuperClassRule.java")
      .withCheck(new AvoidSuperClassRule())
      // As external sources are required to run the rule ('symbolType' used in custom rule, which is
      // part of the semantic API), the test requires external dependencies in order to be run correctly.
      .withClassPath(FilesUtils.getClassPath("target/test-jars"))
      .verifyIssues();
  }
}
