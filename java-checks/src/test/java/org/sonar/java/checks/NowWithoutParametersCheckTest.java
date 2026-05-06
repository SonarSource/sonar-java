package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class NowWithoutParametersCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/NowWithoutParametersCheckSample.java"))
      .withCheck(new NowWithoutParametersCheck())
      .verifyIssues();
  }
}
