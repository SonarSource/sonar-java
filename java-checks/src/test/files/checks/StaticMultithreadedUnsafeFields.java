import java.text.SimpleDateFormat;
import java.util.Date;

public class TestClass {

  public static class ConcreteCalendar extends java.util.Calendar {
  }

  private static int field; // Compliant

  private java.text.SimpleDateFormat format1; // Compliant
  private static java.text.SimpleDateFormat format2; // Noncompliant [[sc=45;ec=52]] {{Make "format2" an instance variable.}}
  private static java.text.SimpleDateFormat format3; // Noncompliant [[sc=45;ec=52]] {{Make "format3" an instance variable.}}
  public static java.text.SimpleDateFormat format4; // Noncompliant [[sc=44;ec=51]] {{Make "format4" an instance variable.}}
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
}
