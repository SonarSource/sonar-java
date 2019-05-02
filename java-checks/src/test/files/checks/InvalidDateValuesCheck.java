package java.util;

import static java.util.Calendar.MONTH;

class A {

  public final static int MINUTE = Calendar.MINUTE;
  boolean b;

  int foo() {
    Date d = new Date();
    d.setDate(25);
    d.setDate(32);// Noncompliant [[sc=15;ec=17]] {{"32" is not a valid value for "setDate" method.}}
    d.setYear(2014);
    d.setMonth(11);
    d.setMonth(12); // Noncompliant {{"12" is not a valid value for "setMonth" method.}}
    d.setHours(23);
    d.setHours(24); // Noncompliant {{"24" is not a valid value for "setHours" method.}}
    d.setMinutes(59);
    d.setMinutes(61); // Noncompliant {{"61" is not a valid value for "setMinutes" method.}}
    d.setSeconds(61);
    d.setSeconds(63);// Noncompliant {{"63" is not a valid value for "setSeconds" method.}}
    d.setSeconds(-1);// Noncompliant {{"-1" is not a valid value for "setSeconds" method.}}
    java.sql.Date d1;
    d1.setHours(23);
    d1.setHours(24); // Noncompliant {{"24" is not a valid value for "setHours" method.}}
    d1.setMinutes(59);
    d1.setMinutes(61);// Noncompliant {{"61" is not a valid value for "setMinutes" method.}}
    d1.setSeconds(61);
    d1.setSeconds(63);// Noncompliant {{"63" is not a valid value for "setSeconds" method.}}
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MONTH, 11);
    cal.set(Calendar.MONTH, 12);// Noncompliant {{"12" is not a valid value for setting "MONTH".}}
    cal.set(MONTH, 12);// Noncompliant {{"12" is not a valid value for setting "MONTH".}}
    cal.set(2, 12);// literal arguments aren't supported
    cal.set(Calendar.DAY_OF_MONTH, 11);
    cal.set(Calendar.DAY_OF_MONTH, 32);// Noncompliant {{"32" is not a valid value for setting "DAY_OF_MONTH".}}
    cal.set(Calendar.HOUR_OF_DAY, 11);
    cal.set(Calendar.HOUR_OF_DAY, 24);// Noncompliant {{"24" is not a valid value for setting "HOUR_OF_DAY".}}
    cal.set(Calendar.MINUTE, 59);
    cal.set(Calendar.MINUTE, 61);// Noncompliant {{"61" is not a valid value for setting "MINUTE".}}
    cal.set(A.MINUTE, 61);// support only Calendar members
    cal.set(Calendar.SECOND, 61);
    cal.set(Calendar.SECOND, 63);// Noncompliant {{"63" is not a valid value for setting "SECOND".}}
    cal.set(Calendar.HOUR_OF_DAY, -2);// Noncompliant {{"-2" is not a valid value for setting "HOUR_OF_DAY".}}
    GregorianCalendar gc = new GregorianCalendar();
    gc = new GregorianCalendar(2015, 11, 31);
    gc = new GregorianCalendar(2015, 12, 31); // Noncompliant {{"12" is not a valid value for setting "month".}}
    gc = new GregorianCalendar(2015, 11, 31);
    gc = new GregorianCalendar(2015, 11, 32); // Noncompliant {{"32" is not a valid value for setting "dayOfMonth".}}
    gc = new GregorianCalendar(2015, 11, 31, 23, 59);
    gc = new GregorianCalendar(2015, 11, 31, 24, 60); // Noncompliant [[sc=46;ec=48]] {{"24" is not a valid value for setting "hourOfDay".}}
    gc = new GregorianCalendar(2015, 11, 31, 23, 59);
    gc = new GregorianCalendar(2015, 11, 31, 23, 61); // Noncompliant {{"61" is not a valid value for setting "minute".}}
    gc = new GregorianCalendar(2015, 11, 31, 23, 59, 61);
    gc = new GregorianCalendar(2015, 11, 31, 23, 59, 63); // Noncompliant {{"63" is not a valid value for setting "second".}}
    // Noncompliant@+1
    gc = new GregorianCalendar(2015, -1, 31, 23, 59, +63); // Noncompliant
    gc = new GregorianCalendar(2015, -foo(), 31, 23, 59, 63); // Noncompliant {{"63" is not a valid value for setting "second".}}

    cal.set(Calendar.DAY_OF_WEEK_IN_MONTH, 11);

    b = cal.get(Calendar.MONTH) == 11;
    b = cal.get(Calendar.MONTH) == foo();
    b = cal.get(Calendar.MONTH) == 12; // Noncompliant {{"12" is not a valid value for "MONTH".}}
    b = cal.get(Calendar.DAY_OF_MONTH) != 11;
    b = cal.get(Calendar.DAY_OF_MONTH) != foo();
    b = cal.get(Calendar.DAY_OF_MONTH) != 32; // Noncompliant {{"32" is not a valid value for "DAY_OF_MONTH".}}
    b = 31 == d.getDate();
    b = foo() == d.getDate();
    b = 32 == d.getDate(); // Noncompliant {{"32" is not a valid value for "getDate".}}
    b = d1.getSeconds() == -1;// Noncompliant {{"-1" is not a valid value for "getSeconds".}}
    b = calendar.get(Calendar.DST_OFFSET) == 0;
  }
}

class RollingCalendar extends GregorianCalendar {

  RollingCalendar() {
    super(); // Compliant
  }

  RollingCalendar(String s) {
    super(2015, 12, 31); // Noncompliant {{"12" is not a valid value for setting "month".}}
  }

  RollingCalendar(int i) {
    super(2015, 11, 31, 1, 1, 1 , /* millis */ 1); // Compliant
  }
}
