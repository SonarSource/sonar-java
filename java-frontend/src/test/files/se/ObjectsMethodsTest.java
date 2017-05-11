package java.util;

import javax.annotation.CheckForNull;
import java.util.Objects;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class ObjectsNullCheck {
  Object o;

  public void parameterMaybeNullable(Object a) {
    Object x = checkForNullMethod();
    if (a.equals(x)) {
      x.toString(); // Compliant: x cannot be null hereafter because of equality
    }
  }

  public void parameterNoLongerNullable(Object a) {
    Object x = checkForNullMethod();
    if (Objects.nonNull(x)) {
      x.toString(); // Compliant: x was checked for non null
    } else {
      x.logNull(); // Noncompliant {{A "NullPointerException" could be thrown; "x" is nullable here.}}
    }
  }

  public void parameterStillNullable(Object a) {
    Object x = checkForNullMethod();
    if (Objects.isNull(x)) {
      log("was null");
    } else {
      x.toString(); // Compliant: x was checked for non null
    }
  }

  public void testRequireNull(Supplier<String> supplier) {
    Object x = checkForNullMethod();
    Objects.requireNonNull(x);
    x.toString(); // Compliant: x was checked for non null
    Object y = checkForNullMethod();
    Objects.requireNonNull(y, "Should not be null!");
    y.toString(); // Compliant: y was checked for non null
    Object z = checkForNullMethod();
    Objects.requireNonNull(z, supplier);
    z.toString(); // Compliant: z was checked for non null
    Object v = checkForNullMethod();
    requireNonNull(v);
    v.toString(); // Compliant
    Object w = checkForNullMethod();
    w.toString(); // Noncompliant
  }

  @CheckForNull
  private Object checkForNullMethod() {
    if (o == null) {
      return null;
    }
    return o;
  }
}
