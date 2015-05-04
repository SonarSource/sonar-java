package org.sonar.java.checks;

import org.junit.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

public class NullPointerDereferenceCheckTest {

  @Test
  public void testName() throws Exception {
    JavaCheckVerifier.verify("src/test/files/checks/NullPointerCheck.java", new NullPointerDereferenceCheck());
  }

}