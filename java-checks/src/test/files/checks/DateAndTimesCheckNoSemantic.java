import java.util.Date;
import java.util.Calendar;

// java.util.Date and java.util.Calendar subclasses
import java.sql.Date;
import java.util.GregorianCalendar;

import java.util.Locale;

import org.joda.time.DateTime; // Noncompliant {{Use the Java 8 Date and Time API instead.}}
import org.joda.time.*; // Noncompliant {{Use the Java 8 Date and Time API instead.}}

import java.time.LocalDateTime;
import java.time.Instant;

import static java.util.Date.from;
import static java.sql.Date.from;
import static org.joda.time.Minutes.minutesBetween; // Noncompliant

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
