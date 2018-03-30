import org.apache.commons.lang.time.DateUtils;
import java.util.Calendar;
import java.util.Date;

import static org.apache.commons.lang.time.DateUtils.truncate;

class A {
  public void foo(Date date, Calendar calendar, Object object, int field) {
    DateUtils.truncate(date, field);      // Noncompliant [[sc=15;ec=23]] {{Use "ZonedDateTime.truncatedTo" instead.}}
    DateUtils.truncate(calendar, field);  // Noncompliant
    DateUtils.truncate(object, field);    // Noncompliant
    truncate(date, field);      // Noncompliant
    truncate(calendar, field);  // Noncompliant
    truncate(object, field);    // Noncompliant
  }
}

class B {
  public void truncate(Date date, int field) {
  }
  
  public void foo(Date date, int field) {
    truncate(date, field);  // Compliant
  }
}
