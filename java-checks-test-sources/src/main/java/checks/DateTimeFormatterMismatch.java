package checks;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.util.Locale;

public class DateTimeFormatterMismatch {
  public void createUsingPatterns() {
    DateTimeFormatter.ofPattern("Y-ww"); // Compliant
    DateTimeFormatter.ofPattern("YY-ww"); // Compliant
    DateTimeFormatter.ofPattern("YYY-ww"); // Compliant
    DateTimeFormatter.ofPattern("YYY-ww"); // Compliant
    DateTimeFormatter.ofPattern("YYYY-ww"); // Compliant
    DateTimeFormatter.ofPattern("Y-ww", Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern("YY-ww", Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern("YYY-ww", Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern("YYY-ww", Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern("YYYY-ww", Locale.ENGLISH); // Compliant


    //The bad stuff
    DateTimeFormatter.ofPattern("y-ww"); // Noncompliant
    DateTimeFormatter.ofPattern("yy-ww"); // Noncompliant
    DateTimeFormatter.ofPattern("yyy-ww"); // Noncompliant
    DateTimeFormatter.ofPattern("yyyy-ww"); // Noncompliant
    DateTimeFormatter.ofPattern("y-ww", Locale.ENGLISH); // Noncompliant
    DateTimeFormatter.ofPattern("yy-ww", Locale.ENGLISH); // Noncompliant
    DateTimeFormatter.ofPattern("yyy-ww", Locale.ENGLISH); // Noncompliant
    DateTimeFormatter.ofPattern("yyyy-ww", Locale.ENGLISH); // Noncompliant


    DateTimeFormatter.ofPattern("w"); // Compliant
    DateTimeFormatter.ofPattern("ww"); // Compliant
    DateTimeFormatter.ofPattern("w", Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern("ww", Locale.ENGLISH); // Compliant


    DateTimeFormatter.ofPattern("u-ww"); // Noncompliant
    DateTimeFormatter.ofPattern("uu-ww"); // Noncompliant
    DateTimeFormatter.ofPattern("uuuu-ww"); // Noncompliant
    DateTimeFormatter.ofPattern("u-ww", Locale.ENGLISH); // Noncompliant
    DateTimeFormatter.ofPattern("uu-ww", Locale.ENGLISH); // Noncompliant
    DateTimeFormatter.ofPattern("uuuu-ww", Locale.ENGLISH); // Noncompliant
  }

  public void createUsingBuilder() {
    new DateTimeFormatterBuilder() // Noncompliant
      .appendValue(ChronoField.YEAR, 4)
      .appendLiteral('-')
      .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 2)
      .toFormatter();

    new DateTimeFormatterBuilder() // Noncompliant
      .appendValue(WeekFields.ISO.weekBasedYear(), 4)
      .appendLiteral('-')
      .appendValue(ChronoField.ALIGNED_WEEK_OF_YEAR, 2)
      .toFormatter();

    new DateTimeFormatterBuilder() // Compliant
      .appendValue(WeekFields.ISO.weekBasedYear(), 4)
      .appendLiteral('-')
      .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 2)
      .toFormatter();

    new DateTimeFormatterBuilder() // Compliant
      .appendValue(ChronoField.YEAR, 4)
      .appendLiteral('-')
      .appendValue(ChronoField.ALIGNED_WEEK_OF_YEAR, 2)
      .toFormatter();
  }
}
