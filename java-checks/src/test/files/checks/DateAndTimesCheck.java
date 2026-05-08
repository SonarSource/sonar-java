import java.util.Date;
import java.util.Locale;
import java.util.Calendar;
import org.joda.time.DateTime;

class A {
  void javaUtil() {
    Date now = new Date(); // Noncompliant {{Use the Java 8 Date and Time API instead.}}
    //         ^^^^^^^^^^
    now = new Date(1499159427440L); // Noncompliant
    //    ^^^^^^^^^^^^^^^^^^^^^^^^
    DateFormat df = new SimpleDateFormat("dd.MM.yyyy");

    Calendar christmas  = Calendar.getInstance(); // Noncompliant
    //                    ^^^^^^^^^^^^^^^^^^^^^^
    christmas = Calendar.getInstance(Locale.CANADA); // Noncompliant
    //          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    christmas.setTime(df.parse("25.12.2020"));
  }

  void jodaTime() {
    DateTime dt = new DateTime(); // Noncompliant {{Use the Java 8 Date and Time API instead.}}
    //            ^^^^^^^^^^^^^^
  }
}
