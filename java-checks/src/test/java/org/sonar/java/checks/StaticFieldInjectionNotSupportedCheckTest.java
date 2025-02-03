package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class StaticFieldInjectionNotSupportedCheckTest {

  @Test
  void test_compiling(){
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/StaticFieldInjectionNotSupportedCheckSample.java"))
      .withCheck(new StaticFieldInjectionNotSupportedCheck())
      .verifyIssues();
  }
}
