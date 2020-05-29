package org.sonar.java.checks.tests;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

import static org.sonar.java.CheckTestUtils.testSourcesPath;

class SimplifiableChainedAssertJAssertionsCheckTest {
  @Test
  void test() {
    JavaCheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/SimplifiableChainedAssertJAssertionsCheckTest.java"))
      .withCheck(new SimplifiableChainedAssertJAssertionsCheck())
      .verifyIssues();
  }
}
