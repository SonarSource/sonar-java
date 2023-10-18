package org.sonar.java.checks.spring;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class ValueAnnotationShouldInjectPropertyOrSpELCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/spring/ValueAnnotationShouldInjectPropertyOrSpELCheckSample.java"))
      .withCheck(new ValueAnnotationShouldInjectPropertyOrSpElCheck())
      .verifyIssues();
  }

}
