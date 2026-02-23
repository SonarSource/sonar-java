package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class ScopedValueStableReferenceCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/ScopedValueStableReferenceCheckSample.java"))
      .withCheck(new ScopedValueStableReferenceCheck())
      .withJavaVersion(25)
      .verifyIssues();
  }

  @Test
  void test_java_24() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/ScopedValueStableReferenceCheckSample.java"))
      .withCheck(new ScopedValueStableReferenceCheck())
      .withJavaVersion(24)
      .verifyNoIssues();
  }

}
