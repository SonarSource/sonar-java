package org.sonar.java.checks.spring;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class StaticFieldInjectionNotSupportedCheckTest {

  @Test
  void test_compiling(){
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/spring/StaticFieldInjectionNotSupportedCheckSample.java"))
      .withCheck(new StaticFieldInjectionNotSupportedCheck())
      .verifyIssues();
  }
}
