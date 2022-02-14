package checks.tests;

import org.testng.annotations.Test;

public class TestStabilityCheck {

  @Test(successPercentage = 80, invocationCount = 10)  // Noncompliant[[sc=9;ec=31]]{{Make this test stable and remove this "successPercentage" argument.}}
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
