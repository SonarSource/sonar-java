package org.sonar.java.checks.spring;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;


import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class ValueAnnotationShouldInjectPropertyOrSpELCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/spring/ValueAnnotationShouldInjectPropertyOrSpELCheck.java"))
      .withCheck(new ValueAnnotationShouldInjectPropertyOrSpElCheck())
      .verifyIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/spring/ValueAnnotationShouldInjectPropertyOrSpELCheck.java"))
      .withCheck(new ValueAnnotationShouldInjectPropertyOrSpElCheck())
      .verifyIssues();
  }

}
