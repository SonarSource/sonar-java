package checks.tests;

public class AssertionsWithoutMessageCheckSample_Testng75 {
  void foo() {
    org.testng.Assert.assertThrows(() -> {}); // Compliant
    org.testng.Assert.assertThrows(Exception.class, () -> {}); // Compliant
    org.testng.Assert.expectThrows(Exception.class, () -> {}); // Compliant
  }
}
