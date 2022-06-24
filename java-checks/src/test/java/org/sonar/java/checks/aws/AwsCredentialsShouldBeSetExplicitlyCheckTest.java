package org.sonar.java.checks.aws;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;


class AwsCredentialsShouldBeSetExplicitlyCheckTest {
  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/aws/AwsCredentialsShouldBeSetExplicitlyCheck.java"))
      .withCheck(new AwsCredentialsShouldBeSetExplicitlyCheck())
      .verifyIssues();
  }

}
