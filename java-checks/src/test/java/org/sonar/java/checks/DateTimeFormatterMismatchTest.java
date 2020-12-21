package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.CheckTestUtils;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

class DateTimeFormatterMismatchTest {
  @Test
  void test() {
    JavaCheckVerifier.newVerifier()
      .onFile(CheckTestUtils.testSourcesPath("checks/DateTimeFormatterMismatch.java"))
      .withCheck(new DateTimeFormatterMismatch())
      .verifyIssues();
  }
}
