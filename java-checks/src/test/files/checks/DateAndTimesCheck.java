import java.util.Date;
import java.util.Locale;
import java.util.Calendar;

class A {
  void foo() {
    Date now = new Date(); // Noncompliant {{Use the Java 8 Date and Time API instead.}}
    now = new Date(1499159427440L); // Noncompliant
    DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
    Calendar christmas  = Calendar.getInstance();  // Noncompliant
    christmas = Calendar.getInstance(Locale.CANADA); // Noncompliant
    christmas.setTime(df.parse("25.12.2020"));
  }
}
