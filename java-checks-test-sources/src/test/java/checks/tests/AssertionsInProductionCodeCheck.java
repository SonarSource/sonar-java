package checks.tests;

import static org.junit.Assert.assertEquals;

public class AssertionsInProductionCodeCheck {

  void method_with_assertions_in_package_name_containing_test() {
    assertEquals(0, 0); // Compliant
  }

}
