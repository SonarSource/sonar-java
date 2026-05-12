import java.util.Date; // Noncompliant {{Use the Java 8 Date and Time API instead.}}
import java.util.Calendar; // Noncompliant

// java.util.Date and java.util.Calendar subclasses
import java.sql.Date; // Noncompliant
import java.util.GregorianCalendar; // Noncompliant

import java.util.Locale; // Compliant (locale can be used in other contexts than date and time)

import org.joda.time.DateTime; // Noncompliant {{Use the Java 8 Date and Time API instead.}}
import org.joda.time.*; // Noncompliant {{Use the Java 8 Date and Time API instead.}}

import java.time.LocalDateTime; // Compliant
import java.time.Instant; // Compliant

import static java.util.Date.from; // Noncompliant
import static java.sql.Date.from; // Compliant (limitation)

class A {
  void javaUtil() {
    Date now = new Date(); // Noncompliant
    //         ^^^^^^^^^^
    Date date = new Date(1499159427440L); // Noncompliant
    Calendar christmas  = Calendar.getInstance(); // Noncompliant
    //                    ^^^^^^^^^^^^^^^^^^^^^^
    Date epochDate = from(Instant.EPOCH);
  }
}
