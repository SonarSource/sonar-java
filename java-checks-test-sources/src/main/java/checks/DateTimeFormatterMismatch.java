package checks;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Locale;

public class DateTimeFormatterMismatch {
  private static final String COMPLIANT_PATTERN = "Y-ww";
  private static final String NON_COMPLIANT_PATTERN = "y-ww";
  private static final String IRRELEVANT_PATTERN = "m";
  private static final String NON_INITIALIZED_PATTERN = null;
  private String compliantAndNonFinalPattern = "Y-ww";
  private String nonCompliantAndNonFinalPattern = "y-ww";

  public void createUsingPatterns() {
    DateTimeFormatter.ofPattern(COMPLIANT_PATTERN); // Compliant
    DateTimeFormatter.ofPattern(IRRELEVANT_PATTERN); // Compliant
    DateTimeFormatter.ofPattern(NON_INITIALIZED_PATTERN); // Compliant
    DateTimeFormatter.ofPattern(compliantAndNonFinalPattern); // Compliant
    DateTimeFormatter.ofPattern(nonCompliantAndNonFinalPattern); // Compliant FN
    DateTimeFormatter.ofPattern("Y-ww"); // Compliant
    DateTimeFormatter.ofPattern("YY-ww"); // Compliant
    DateTimeFormatter.ofPattern("YYY-ww"); // Compliant
    DateTimeFormatter.ofPattern("YYYY-ww"); // Compliant
    DateTimeFormatter.ofPattern(COMPLIANT_PATTERN, Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern(IRRELEVANT_PATTERN, Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern(NON_INITIALIZED_PATTERN, Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern("Y-ww", Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern("YY-ww", Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern("YYY-ww", Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern("YYYY-ww", Locale.ENGLISH); // Compliant


    //The bad stuff
    DateTimeFormatter.ofPattern(NON_COMPLIANT_PATTERN); // Noncompliant
    DateTimeFormatter.ofPattern("y-ww"); // Noncompliant
    DateTimeFormatter.ofPattern("yy-ww"); // Noncompliant
    DateTimeFormatter.ofPattern("yyy-ww"); // Noncompliant
    DateTimeFormatter.ofPattern("yyyy-ww"); // Noncompliant
    DateTimeFormatter.ofPattern(NON_COMPLIANT_PATTERN, Locale.ENGLISH); // Noncompliant
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
    new DateTimeFormatterBuilder()
      .appendValue(ChronoField.YEAR, 4) // Noncompliant [[sc=20;ec=36;secondary=+2]] {{Change this year format to use the week-based year instead.}}
      .appendLiteral('-')
      .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 2)
      .toFormatter();

    new DateTimeFormatterBuilder()
      .appendValue(WeekFields.ISO.weekBasedYear(), 4) // Noncompliant [[sc=20;ec=50;secondary=+2]] {{Change this year format to use ChronoField.YEAR instead.}}
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

    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
      .appendValue(ChronoField.YEAR, 4) // Noncompliant [[sc=20;ec=36;secondary=+1]] {{Change this year format to use the week-based year instead.}}
      .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 2)
      .appendLiteral('-');

    builder.toFormatter();

    new DateTimeFormatterBuilder() // Compliant
      .appendValue(WeekFields.ISO.weekBasedYear(), 4)
      .toFormatter();

    new DateTimeFormatterBuilder() // Compliant
      .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 2)
      .toFormatter();

    DateTimeFormatterBuilder builder2 = new DateTimeFormatterBuilder();
    builder2.appendValue(ChronoField.YEAR, 4); // Compliant FN - consecutive calls on builders are not considered
    builder2.appendLiteral('-');
    builder2.appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 2);
    builder2.toFormatter();

    new DateTimeFormatterBuilder()
      .appendValue(FakeChronoField.YEAR, 4) // Compliant FN - Ignore cases where a weird custom temporal field is used
      .appendLiteral('-')
      .appendValue(FakeChronoField.ALIGNED_WEEK_OF_YEAR, 2)
      .toFormatter();

  }

  static private class FakeChronoField {
    private static final TemporalField YEAR = ChronoField.YEAR_OF_ERA;
    private static final TemporalField ALIGNED_WEEK_OF_YEAR = ChronoField.DAY_OF_YEAR;
  }
}
