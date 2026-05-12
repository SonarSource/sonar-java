import java.util.*;

class A {
  void javaUtil() {
    Date now = new Date(); // Noncompliant
    //         ^^^^^^^^^^
    Calendar christmas  = Calendar.getInstance(); // Noncompliant
    //                    ^^^^^^^^^^^^^^^^^^^^^^
    Timestamp timestamp = new Timestamp(1735689600000L); // Compliant, the rule ignores this case
    GregorianCalendar calendar = GregorianCalendar.getInstance(); // Noncompliant {{Use the Java 8 Date and Time API instead.}}
    //                           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }
}
