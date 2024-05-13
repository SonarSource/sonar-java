package checks.tests;

import org.testng.annotations.Test;

public class TestStabilityCheckSample {

  @Test(successPercentage = 80, invocationCount = 10) // Noncompliant {{Make this test stable and remove this "successPercentage" argument.}}
//      ^^^^^^^^^^^^^^^^^^^^^^
  public void flakyTest() {
  }

  @Test // Compliant
  public void normalTest() {
  }

  @Test(invocationCount = 10) // Compliant
  public void normalTestDoubled() {
  }

  @org.junit.Test
  public void junitTest() {

  }

}
