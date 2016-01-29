package java.util;

import java.util.Objects;

public class ObjectsNullCheck {
  
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
      x.logNull(); // Noncompliant {{NullPointerException might be thrown as 'x' is nullable here}}
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
  }
}

// Needed to ensure that all methods of Objects are declared (for tests with Java version prior to 1.8)

class Objects {
  public static boolean isNull(Object obj) {
    return obj == null;
  }

  public static boolean nonNull(Object obj) {
    return obj != null;
  }
  
  public static <T> T  requireNonNull(T obj) {
    if (obj == null) {
      throw new NullPointerException();
    }
  }
  
  public static <T> T  requireNonNull(T obj, String message) {
    if (obj == null) {
      throw new NullPointerException(message);
    }
  }
  
  public static <T> T  requireNonNull(T obj, Supplier<String> supplier) {
    if (obj == null) {
      throw new NullPointerException(supplier.get());
    }
  }
}
