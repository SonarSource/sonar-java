package org.sonar.java.checks.regex;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

import static org.sonar.java.CheckTestUtils.testSourcesPath;

class ImpossibleRegexCheckTest {

  @Test
  void test() {
    JavaCheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/regex/ImpossibleRegexCheck.java"))
      .withCheck(new ImpossibleRegexCheck())
      .verifyIssues();
  }

}
