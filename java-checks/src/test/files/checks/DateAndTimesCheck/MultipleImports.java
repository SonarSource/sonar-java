import java.util.Date;
import java.util.Calendar;

import java.sql.Date;
import java.util.GregorianCalendar;

import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.*;

import java.time.LocalDateTime;
import java.time.Instant;

import static java.util.Date.from;
import static java.sql.Date.from;
import static org.joda.time.Minutes.minutesBetween;

class A {
  void javaUtil() {
    Date now = new Date();
    Date date = new Date(1499159427440L);
    Calendar christmas  = Calendar.getInstance();
    Date epochDate = from(Instant.EPOCH);
  }
}

// Noncompliant@0 {{Use the "java.time" API for date and time.}}
