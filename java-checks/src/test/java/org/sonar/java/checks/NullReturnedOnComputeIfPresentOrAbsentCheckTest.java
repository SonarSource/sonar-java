package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

import static org.sonar.java.CheckTestUtils.testSourcesPath;

public class NullReturnedOnComputeIfPresentOrAbsentCheckTest {
  @Test
  void test() {
    JavaCheckVerifier
      .newVerifier()
      .onFile(testSourcesPath("checks/NullReturnedOnComputeIfPresentOrAbsent.java"))
      .withCheck(new NullReturnedOnComputeIfPresentOrAbsentCheck())
      .verifyIssues();
  }
}
