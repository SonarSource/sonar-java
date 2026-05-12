import java.util.*; // Compliant, java.util.* is not directly flagged by the rule

class A {
  void javaUtil() {
    Date now = new Date(); // Noncompliant {{Use the Java 8 Date and Time API instead.}}
    //         ^^^^^^^^^^
    Calendar christmas  = Calendar.getInstance(); // Noncompliant
    //                    ^^^^^^^^^^^^^^^^^^^^^^
    Calendar gregorianCalendar = GregorianCalendar.getInstance(); // Noncompliant
    Timestamp timestamp = new Timestamp(1735689600000L); // Compliant, the rule ignores this case (known limitation)
  }
}
