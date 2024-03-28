package checks;

import lombok.val;

public class SillyEqualsCheckSample {

  public void method(Object object) {
    // Compliant
    object.equals();
  }

  boolean foo(String x) {
    lombok.val y = "Hello World";
    return x.equals(y); // Noncompliant - FP - removed by the lombok filter
  }

}

class ExtendsUnknown extends Unknown {
  @Override
  public boolean equals(Object o) {
    if (super.equals(o)) { // Compliant, super is unknown
      return false;
    }
    return o.toString().equals("abc");
  }

  public void unknownArg(Unknown unknown, Object object, Unknown[] unknownArray) {
    object.equals(unknown); // Compliant
    unknown.equals(object); // Compliant

    object.equals(unknownArray); // Compliant
    unknownArray.equals(object); // Compliant

    unknownArray.equals(unknown); // Compliant
    unknown.equals(unknownArray); // Compliant
  }
}
