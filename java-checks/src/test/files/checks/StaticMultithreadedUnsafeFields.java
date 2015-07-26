public class TestClass {

  public static class ConcreteCalendar extends java.util.Calendar {
  }

  private static int field; // Compliant

  private java.text.SimpleDateFormat format1; // Compliant
  private static java.text.SimpleDateFormat format2; // Noncompliant {{Make "format2" an instance variable.}}

  private java.util.Calendar calendar1; // Compliant
  private static java.util.Calendar calendar2; // Noncompliant {{Make "calendar2" an instance variable.}}
  private static ConcreteCalendar calendar3; // Noncompliant {{Make "calendar3" an instance variable.}}

}
