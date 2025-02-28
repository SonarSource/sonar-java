package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class FileUsageCheckTest {
  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/FileUsageCheckSample.java"))
      .withCheck(new FileUsageCheck())
      .withJavaVersion(12)
      .verifyIssues();
  }
}
