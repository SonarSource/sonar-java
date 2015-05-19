import junit.framework.TestCase;

import javax.annotation.Nullable;

public class AssertionsInTestsCheckTestJunit3 extends TestCase {

  public void testCompliant() {
    org.junit.Assert.assertTrue(true);
  }

  public void testNoncompliant() { // Noncompliant
  }

  @Nullable
  public Test notAtest() {
    compliant1();
  }

}
