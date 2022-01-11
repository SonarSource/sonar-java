/*
 * Copyright (C) 2012-2022 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package org.sonar.samples.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.samples.java.utils.FilesUtils;

class AvoidTreeListRuleTest {

  @Test
  void verify() {

    // Verifies automatically that the check will raise the adequate issues with the expected message
    CheckVerifier.newVerifier()
      .onFile("src/test/files/AvoidTreeListRule.java")
      .withCheck(new AvoidTreeListRule())
      // In order to test this check efficiently, we added the test-jar "org.apache.commons.commons-collections4" to the pom,
      // which is normally not used by the code of our custom plugin.
      // All the classes from this jar will then be read when verifying the ticket, allowing correct type resolution.
      // You have to give the test jar directory to the verifier in order to make it work correctly.
      .withClassPath(FilesUtils.getClassPath("target/test-jars"))
      .verifyIssues();
  }

}
