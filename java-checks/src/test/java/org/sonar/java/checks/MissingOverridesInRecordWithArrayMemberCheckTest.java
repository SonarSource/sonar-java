package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.TestUtils;

class MissingOverridesInRecordWithArrayMemberCheckTest {
  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.testSourcesPath("checks/MissingOverridesInRecordWithArrayMemberCheck.java"))
      .withChecks(new MissingOverridesInRecordWithArrayMemberCheck())
      .verifyIssues();
  }
}
