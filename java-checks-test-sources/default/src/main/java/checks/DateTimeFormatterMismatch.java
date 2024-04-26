package checks;

import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Map;

public class DateTimeFormatterMismatch {

  public void createUsingBuilder() {
    new DateTimeFormatterBuilder()
      .appendValue(ChronoField.YEAR, 4) // Noncompliant {{Change this year format to use the week-based year instead (or the week format to Chronofield.ALIGNED_WEEK_OF_YEAR).}}
//                 ^^^^^^^^^^^^^^^^
      .appendLiteral('-')
      .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 2)
//  ^^^<
      .toFormatter();

    new DateTimeFormatterBuilder()
      .appendValue(ChronoField.MONTH_OF_YEAR, 2)
      .appendLiteral("-")
      .appendValue(ChronoField.YEAR, 4) // Noncompliant {{Change this year format to use the week-based year instead (or the week format to Chronofield.ALIGNED_WEEK_OF_YEAR).}}
//                 ^^^^^^^^^^^^^^^^
      .appendLiteral('-')
      .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 2)
//  ^^^<
      .toFormatter();

    new DateTimeFormatterBuilder()
      .appendLiteral('[')
      .appendValue(ChronoField.YEAR_OF_ERA, 4) // Noncompliant {{Change this year format to use the week-based year instead (or the week format to Chronofield.ALIGNED_WEEK_OF_YEAR).}}
//                 ^^^^^^^^^^^^^^^^^^^^^^^
      .appendLiteral(']')
      .appendLiteral('-')
      .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 2)
//  ^^^<
      .toFormatter();

    new DateTimeFormatterBuilder()
      .appendValue(WeekFields.ISO.weekBasedYear(), 4) // Noncompliant {{Change this year format to use ChronoField.YEAR instead (or the week format to WeekFields.ISO.weekOfWeekBasedYear()).}}
//                 ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      .appendLiteral('-')
      .appendValue(ChronoField.ALIGNED_WEEK_OF_YEAR, 2)
//  ^^^<
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

    new DateTimeFormatterBuilder() // Compliant
      .appendValue(ChronoField.YEAR_OF_ERA, 4)
      .appendLiteral('-')
      .appendValue(ChronoField.ALIGNED_WEEK_OF_YEAR, 2)
      .toFormatter();

    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
      .appendValue(ChronoField.YEAR, 4) // Noncompliant {{Change this year format to use the week-based year instead (or the week format to Chronofield.ALIGNED_WEEK_OF_YEAR).}}
//                 ^^^^^^^^^^^^^^^^
      .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 2)
//  ^^^<
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

    new DateTimeFormatterBuilder()
      .appendValue(FakeChronoField.YEAR, 4) // Compliant FN - Ignore cases where a weird custom temporal field is used
      .appendLiteral('-')
      .appendValue(FakeChronoField.NULL, 2)
      .toFormatter();

    TemporalField customField = new EmptyTemporalFieldImpl();

    new DateTimeFormatterBuilder()
      .appendValue(ChronoField.YEAR, 4) // Compliant FN - Ignore cases where a weird custom temporal field is used
      .appendLiteral('-')
      .appendValue(customField, 2)
      .toFormatter();

    new DateTimeFormatterBuilder()
      .appendValue(WeekFields.ISO.weekBasedYear(), 4) // Noncompliant {{Change this year format to use ChronoField.YEAR instead (or the week format to WeekFields.ISO.weekOfWeekBasedYear()).}}
//                 ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      .appendValue(ChronoField.YEAR_OF_ERA, 4)
      .appendLiteral('-')
      .appendValue(ChronoField.ALIGNED_WEEK_OF_YEAR, 2)
//  ^^^<
      .toFormatter();

    // Matches on appendValue methods with different parameter list
    new DateTimeFormatterBuilder()
      .appendValue(ChronoField.YEAR) // Noncompliant {{Change this year format to use the week-based year instead (or the week format to Chronofield.ALIGNED_WEEK_OF_YEAR).}}
//                 ^^^^^^^^^^^^^^^^
      .appendLiteral('-')
      .appendValue(WeekFields.ISO.weekOfWeekBasedYear())
//  ^^^<
      .toFormatter();

    new DateTimeFormatterBuilder()
      .appendValue(ChronoField.MONTH_OF_YEAR)
      .appendLiteral("-")
      .appendValue(ChronoField.YEAR) // Noncompliant {{Change this year format to use the week-based year instead (or the week format to Chronofield.ALIGNED_WEEK_OF_YEAR).}}
//                 ^^^^^^^^^^^^^^^^
      .appendLiteral('-')
      .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 1, 2, SignStyle.NORMAL)
//  ^^^<
      .toFormatter();
    new DateTimeFormatterBuilder()
      .appendValue(ChronoField.YEAR, 2, 4, SignStyle.NORMAL) // Noncompliant {{Change this year format to use the week-based year instead (or the week format to Chronofield.ALIGNED_WEEK_OF_YEAR).}}
//                 ^^^^^^^^^^^^^^^^
      .appendLiteral('-')
      .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 1, 2, SignStyle.NORMAL)
//  ^^^<
      .toFormatter();

    new DateTimeFormatterBuilder()
      .appendValue(ChronoField.MONTH_OF_YEAR, 2, 4, SignStyle.NORMAL)
      .appendLiteral("-")
      .appendValue(ChronoField.YEAR, 4) // Noncompliant {{Change this year format to use the week-based year instead (or the week format to Chronofield.ALIGNED_WEEK_OF_YEAR).}}
//                 ^^^^^^^^^^^^^^^^
      .appendLiteral('-')
      .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 1, 2, SignStyle.NORMAL)
//  ^^^<
      .toFormatter();

    new DateTimeFormatterBuilder()
      .appendValue(WeekFields.ISO.weekBasedYear(), 4)
      .appendInstant()
      .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 1, 2, SignStyle.NORMAL)
      .toFormatter();
  }

  private static class FakeChronoField {
    private static final TemporalField YEAR = ChronoField.YEAR_OF_ERA;
    private static final TemporalField ALIGNED_WEEK_OF_YEAR = ChronoField.DAY_OF_YEAR;
    private static final TemporalField NULL = null;
  }

  private class EmptyTemporalFieldImpl implements TemporalField {

    @Override
    public String getDisplayName(Locale locale) {
      return null;
    }

    @Override
    public TemporalUnit getBaseUnit() {
      return null;
    }

    @Override
    public TemporalUnit getRangeUnit() {
      return null;
    }

    @Override
    public ValueRange range() {
      return null;
    }

    @Override
    public boolean isDateBased() {
      return false;
    }

    @Override
    public boolean isTimeBased() {
      return false;
    }

    @Override
    public boolean isSupportedBy(TemporalAccessor temporalAccessor) {
      return false;
    }

    @Override
    public ValueRange rangeRefinedBy(TemporalAccessor temporalAccessor) {
      return null;
    }

    @Override
    public long getFrom(TemporalAccessor temporalAccessor) {
      return 0;
    }

    @Override
    public <R extends Temporal> R adjustInto(R r, long l) {
      return null;
    }

    @Override
    public TemporalAccessor resolve(Map<TemporalField, Long> fieldValues, TemporalAccessor partialTemporal, ResolverStyle resolverStyle) {
      return null;
    }
  }
}
