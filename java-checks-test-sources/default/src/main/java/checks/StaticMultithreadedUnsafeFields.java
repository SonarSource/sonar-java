package checks;

import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Date;

public class StaticMultithreadedUnsafeFields {

  public static class ConcreteCalendar extends java.util.Calendar {
    @Override public void computeTime() { }
    @Override public void computeFields() { }
    @Override public void add(int field, int amount) { }
    @Override public void roll(int field, boolean up) { }
    @Override public int getMinimum(int field) { return 0; }
    @Override public int getMaximum(int field) { return 0; }
    @Override public int getGreatestMinimum(int field) { return 0; }
    @Override public int getLeastMaximum(int field) { return 0; }
  }

  private static final DateFormat[] ARRAY = {}; // Compliant

  private static final DateFormat DATE_FORMAT_1 = DateFormat.getDateInstance(); // Noncompliant {{Make "DATE_FORMAT_1" an instance variable.}}
  private static final DateFormat DATE_FORMAT_2 = bar(); // Noncompliant {{Make "DATE_FORMAT_2" an instance variable.}}
  private static final DateFormat DATE_FORMAT_3 = qix(); // Compliant
  private static final DateFormat DATE_FORMAT_4 = ARRAY[0]; // Compliant

  private static int field; // Compliant

  private java.text.SimpleDateFormat format1; // Compliant
  private static java.text.SimpleDateFormat format2; // Noncompliant {{Make "format2" an instance variable.}}
//                                          ^^^^^^^
  private static java.text.SimpleDateFormat format3; // Noncompliant {{Make "format3" an instance variable.}}
//                                          ^^^^^^^
  public static java.text.SimpleDateFormat format4; // Noncompliant {{Make "format4" an instance variable.}}
//                                         ^^^^^^^
  private static java.text.DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); // Noncompliant
  private static java.text.DateFormat dateFormat2 = null;

  private java.util.Calendar calendar1; // Compliant
  private static java.util.Calendar calendar2; // Noncompliant {{Make "calendar2" an instance variable.}}
  private static ConcreteCalendar calendar3; // Noncompliant {{Make "calendar3" an instance variable.}}
  private static javax.xml.xpath.XPath xPath; // Noncompliant {{Make "xPath" an instance variable.}}
  private static javax.xml.validation.SchemaFactory schemaFactory; // Noncompliant {{Make "schemaFactory" an instance variable.}}

  static java.text.SimpleDateFormat synchronizedDateFormat = new SimpleDateFormat("MM-dd-yyyy-hhmmss"); // Compliant
  static java.text.SimpleDateFormat unsynchronizedDateFormat = new SimpleDateFormat("MM-dd-yyyy-hhmmss"); // Noncompliant {{Make "unsynchronizedDateFormat" an instance variable.}}

  void foo() {
    synchronized (synchronizedDateFormat) {
      synchronizedDateFormat.format(new Date()); // synchronizedDateFormat synchronized
      unsynchronizedDateFormat.format(new Date());
    }
    format2.format(new Date()); // format2 not synchronized
    synchronized (new Date()) {
      format3.format(new Date()); // format3 not synchronized
    }
  }

  static SimpleDateFormat bar() { return null; }
  static DateFormat qix() { return null; }
}
