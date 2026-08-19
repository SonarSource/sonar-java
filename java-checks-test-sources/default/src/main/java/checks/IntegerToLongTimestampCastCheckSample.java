package checks;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

class IntegerToLongTimestampCastCheckSample {

  static final int INT_CONSTANT = 1234567890;

  void noncompliantImplicitWidening() {
    int intVar = 1000;
    new Date(intVar); // Noncompliant {{Use a "long" value to represent this timestamp.}}
//          ^^^^^^
    Instant.ofEpochSecond(intVar); // Noncompliant
    Instant.ofEpochMilli(intVar); // Noncompliant
    new Timestamp(intVar); // Noncompliant
  }

  void noncompliantExplicitCast() {
    int intVar = 1000;
    new Date((long) intVar); // Noncompliant
    Instant.ofEpochSecond((long) intVar); // Noncompliant
    Instant.ofEpochMilli((long) intVar); // Noncompliant
    new Timestamp((long) intVar); // Noncompliant
  }

  void noncompliantArithmeticOverflow() {
    int days = 365;
    new Date((long) (days * 24 * 60 * 60 * 1000)); // Noncompliant
  }

  void noncompliantCalendar() {
    int intVar = 1000;
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(intVar); // Noncompliant
    cal.setTimeInMillis((long) intVar); // Noncompliant
  }

  void noncompliantGregorianCalendar() {
    int intVar = 1000;
    GregorianCalendar cal = new GregorianCalendar();
    cal.setTimeInMillis(intVar); // Noncompliant
  }

  void noncompliantOtherNarrowTypes() {
    short shortVar = 100;
    byte byteVar = 10;
    char charVar = 'A';
    new Date((long) shortVar); // Noncompliant
    new Date((long) byteVar); // Noncompliant
    new Date((long) charVar); // Noncompliant
  }

  void noncompliantMethodReturn() {
    new Date(getSeconds()); // Noncompliant
  }

  void noncompliantIntConstant() {
    Instant.ofEpochSecond(INT_CONSTANT); // Noncompliant
  }

  void noncompliantOfEpochSecondTwoArgs() {
    int intVar = 1000;
    Instant.ofEpochSecond(intVar, 0L); // Noncompliant
  }

  void noncompliantParenthesized() {
    int intVar = 1000;
    new Date((intVar)); // Noncompliant
  }

  void noncompliantIntLiteral() {
    new Date(0); // Noncompliant
  }

  void compliantLongVariable() {
    long longVar = 1234567890L;
    new Date(longVar);
    Instant.ofEpochSecond(longVar);
    Instant.ofEpochMilli(longVar);
    new Timestamp(longVar);
  }

  void compliantCurrentTimeMillis() {
    new Date(System.currentTimeMillis());
  }

  void compliantLongLiteral() {
    new Date(1234567890L);
  }

  void compliantCalendar() {
    long longVar = 1234567890L;
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(longVar);
  }

  void compliantLongMethodReturn() {
    new Date(getMillis());
  }

  void compliantNonTimestampCast() {
    int intVar = 1000;
    long result = (long) intVar;
  }

  int getSeconds() {
    return 1000;
  }

  long getMillis() {
    return System.currentTimeMillis();
  }
}
