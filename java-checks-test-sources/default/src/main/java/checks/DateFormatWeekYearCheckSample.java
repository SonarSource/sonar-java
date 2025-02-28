package checks;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Date;
import java.util.Locale;


class DateFormatWeekYearCheckSample {
  private static final String COMPLIANT_DATE_FORMAT = "yyyy/MM/dd";
  private static final String COMPLIANT_PATTERN = "Y-ww";
  private static final String NON_COMPLIANT_PATTERN = "YYYY/MM/dd";
  private static final String IRRELEVANT_PATTERN = "m";
  private static final String NON_INITIALIZED_PATTERN = null;
  private String compliantAndNonFinalPattern = "Y-ww";
  private String nonCompliantAndNonFinalPattern = "y-ww";

  void quickFixes() throws ParseException {
    Date date = new SimpleDateFormat("yyyy/MM/dd").parse("2015/12/31");
    String result = new SimpleDateFormat("YYYY/MM/dd").format(date); // Noncompliant [[quickfixes=qf1]]
//                                       ^^^^^^^^^^^^
    // fix@qf1 {{Replace year format}}
    // edit@qf1 [[sc=43;ec=47]] {{yyyy}}
    result = new SimpleDateFormat("YYYY").format(date); // Noncompliant [[quickfixes=qf2]]
//                                ^^^^^^
    // fix@qf2 {{Replace year format}}
    // edit@qf2 [[sc=36;ec=40]] {{yyyy}}
    result = new SimpleDateFormat("  Y/MM/dd").format(date); // Noncompliant [[quickfixes=qf3]]
//                                ^^^^^^^^^^^
    // fix@qf3 {{Replace year format}}
    // edit@qf3 [[sc=38;ec=39]] {{y}}
    DateTimeFormatter.ofPattern("YYYY"); // Noncompliant [[quickfixes=qf4]]
//                              ^^^^^^
    // fix@qf4 {{Replace year format}}
    // edit@qf4 [[sc=34;ec=38]] {{yyyy}}
    DateTimeFormatter.ofPattern("  Y/MM/dd"); // Noncompliant [[quickfixes=qf5]]
//                              ^^^^^^^^^^^
    // fix@qf5 {{Replace year format}}
    // edit@qf5 [[sc=36;ec=37]] {{y}}
    DateTimeFormatter.ofPattern("YY"); // Noncompliant [[quickfixes=qf6]]
//                              ^^^^
    // fix@qf6 {{Replace year format}}
    // edit@qf6 [[sc=34;ec=36]] {{yy}}
  }

  void useSimpleDateFormat() throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat();
    sdf = new SimpleDateFormat(COMPLIANT_DATE_FORMAT);
    Date date = new SimpleDateFormat("yyyy/MM/dd").parse("2015/12/31");
    String result = new SimpleDateFormat("YYYY/MM/dd").format(date); // Noncompliant {{Use "yyyy" instead of "YYYY" to output the year.}}
    result = new SimpleDateFormat("YYYY").format(date); // Noncompliant {{Use "yyyy" instead of "YYYY" to output the year.}}
    result = new SimpleDateFormat("  Y/MM/dd").format(date); // Noncompliant {{Use "y" instead of "Y" to output the year.}}
    result = new SimpleDateFormat("yyyy/MM/dd").format(date);   //Yields '2015/12/31' as expected
    result = new SimpleDateFormat("YYYY-ww").format(date); //compliant, 'Week year' is used along with 'Week of year'. result = '2016-01'
    result = new SimpleDateFormat("ww-YYYY").format(date); //compliant, 'Week year' is used along with 'Week of year'. result = '2016-01'
  }

  public void useDateTimeFormatter() {
    DateTimeFormatter.ofPattern(COMPLIANT_PATTERN); // Compliant
    DateTimeFormatter.ofPattern(IRRELEVANT_PATTERN); // Compliant
    DateTimeFormatter.ofPattern(NON_INITIALIZED_PATTERN); // Compliant
    DateTimeFormatter.ofPattern(compliantAndNonFinalPattern); // Compliant
    DateTimeFormatter.ofPattern(nonCompliantAndNonFinalPattern); // Compliant FN Patterns in non-final String are not processed
    DateTimeFormatter.ofPattern("Y-ww"); // Compliant
    DateTimeFormatter.ofPattern("YY-ww"); // Compliant
    DateTimeFormatter.ofPattern("YYY-ww"); // Compliant
    DateTimeFormatter.ofPattern("YYYY-ww"); // Compliant
    DateTimeFormatter.ofPattern(" YY/MM/dd ".trim()); // Compliant FN patterns from method invocations are not processed
    DateTimeFormatter.ofPattern(COMPLIANT_PATTERN, Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern(IRRELEVANT_PATTERN, Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern(NON_INITIALIZED_PATTERN, Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern("Y-ww", Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern("YY-ww", Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern("YYY-ww", Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern("YYYY-ww", Locale.ENGLISH); // Compliant
    DateTimeFormatter.ofPattern(" YY/MM/dd ".trim(), Locale.ENGLISH); // Compliant FN patterns from method invocations are not processed

    DateTimeFormatter.ofPattern("YYYY"); // Noncompliant {{Use "yyyy" instead of "YYYY" to output the year.}}
//                              ^^^^^^
    DateTimeFormatter.ofPattern("  Y/MM/dd"); // Noncompliant {{Use "y" instead of "Y" to output the year.}}
//                              ^^^^^^^^^^^
    DateTimeFormatter.ofPattern("YY"); // Noncompliant {{Use "yy" instead of "YY" to output the year.}}
//                              ^^^^
    DateTimeFormatter.ofPattern("Y"); // Noncompliant {{Use "y" instead of "Y" to output the year.}}
//                              ^^^
    DateTimeFormatter.ofPattern(NON_COMPLIANT_PATTERN); // Noncompliant {{Use "yyyy" instead of "YYYY" to output the year.}}
//                              ^^^^^^^^^^^^^^^^^^^^^
    DateTimeFormatter.ofPattern("YYYY", Locale.ENGLISH); // Noncompliant {{Use "yyyy" instead of "YYYY" to output the year.}}
//                              ^^^^^^
    DateTimeFormatter.ofPattern("  Y/MM/dd", Locale.ENGLISH); // Noncompliant {{Use "y" instead of "Y" to output the year.}}
//                              ^^^^^^^^^^^
    DateTimeFormatter.ofPattern("YY", Locale.ENGLISH); // Noncompliant {{Use "yy" instead of "YY" to output the year.}}
//                              ^^^^
    DateTimeFormatter.ofPattern("Y", Locale.ENGLISH); // Noncompliant {{Use "y" instead of "Y" to output the year.}}
//                              ^^^
    DateTimeFormatter.ofPattern(NON_COMPLIANT_PATTERN, Locale.ENGLISH); // Noncompliant {{Use "yyyy" instead of "YYYY" to output the year.}}
//                              ^^^^^^^^^^^^^^^^^^^^^
  }

  class CompliantChildOfSimpleDateFormat extends SimpleDateFormat {
    public CompliantChildOfSimpleDateFormat() {
      super(); // Compliant
    }
  }

  class GrandChildOfSimpleDateFormat extends SimpleDateFormat {
    public GrandChildOfSimpleDateFormat() {
      super(); // Compliant
    }
  }

  class NonCompliantChildOfSimpleDateFormat extends SimpleDateFormat {
    public NonCompliantChildOfSimpleDateFormat() {
      super("YYYY"); // Noncompliant {{Use "yyyy" instead of "YYYY" to output the year.}}
    }
  }
}
