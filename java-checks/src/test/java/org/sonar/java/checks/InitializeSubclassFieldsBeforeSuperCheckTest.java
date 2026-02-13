package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class InitializeSubclassFieldsBeforeSuperCheckTest {
  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/InitializeSubclassFieldsBeforeSuperSample.java"))
      .withCheck(new InitializeSubclassFieldsBeforeSuperCheck())
      .verifyIssues();
  }

  @Test
  void test_java_24() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/InitializeSubclassFieldsBeforeSuperSample.java"))
      .withCheck(new InitializeSubclassFieldsBeforeSuperCheck())
      .withJavaVersion(24)
      .verifyNoIssues();
  }
}
