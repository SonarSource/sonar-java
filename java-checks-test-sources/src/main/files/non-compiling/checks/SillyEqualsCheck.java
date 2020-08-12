package checks;

import lombok.val;

public class SillyEqualsCheck {

  public void method(Object object) {
    // Compliant
    object.equals();
  }

  boolean foo(String x) {
    lombok.val y = "Hello World";
    return x.equals(y); // Noncompliant - FP - removed by the lombok filter
  }

}
