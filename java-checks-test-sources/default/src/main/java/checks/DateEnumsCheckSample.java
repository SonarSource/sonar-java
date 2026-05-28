package checks;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class DateEnumsCheckSample {

  void noncompliantDateCreation() {
    LocalDate date = LocalDate.of(2024, 1, 15); // Noncompliant {{Use a "java.time.Month" enum constant instead of this int literal.}}
    //                                  ^
    LocalDateTime dateTime = LocalDateTime.of(2024, 10, 15, 1, 20); // Noncompliant {{Use a "java.time.Month" enum constant instead of this int literal.}}
    //                                              ^^
    YearMonth ym = YearMonth.of(2025, 3); // Noncompliant
    //                                ^
    MonthDay md = MonthDay.of(2, 16); // Noncompliant
    //                        ^
    MonthDay monthDay = MonthDay.of(+2, 16); // Compliant; ignored by the rule to keep it simple
    LocalDate ld = LocalDate.of(2024, -1, 15); // Compliant; ignored by the rule to keep it simple
    DayOfWeek day = DayOfWeek.of(2); // Noncompliant {{Use a "java.time.DayOfWeek" enum constant instead of this int literal.}}
    Month month = Month.of(10); // Noncompliant {{Use a "java.time.Month" enum constant instead of this int literal.}}
    //            ^^^^^^^^^^^^
  }

  void compliantDateCreation(int monthNumber) {
    LocalDate date = LocalDate.of(2024, Month.JANUARY, 15);
    LocalDateTime dateTime = LocalDateTime.of(2024, Month.DECEMBER, 25, 10, 30);
    YearMonth yearMonth = YearMonth.of(2024, Month.JUNE);
    MonthDay monthDay = MonthDay.of(Month.FEBRUARY, 16);
    LocalDateTime dateTimeWithVariable = LocalDateTime.of(2024, monthNumber, 25, 10, 30);
    OffsetDateTime offsetDateTime = OffsetDateTime.of(2025, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC); // Compliant; no "of" method available with a Month enum
    ZonedDateTime zdt = ZonedDateTime.of(2026, 12, 21, 3, 33, 33, 4, ZoneId.of("Europe/Paris")); // Compliant; no "of" method available with a Month enum
    DayOfWeek parenthesized = DayOfWeek.of((-3)); // Compliant; ignored by the rule to keep it simple
    DayOfWeek parenthesizedOutside = DayOfWeek.of(-(3)); // Compliant; ignored by the rule to keep it simple
  }

  boolean noncompliantDateManipulation(LocalDate date, DayOfWeek day, Month month) {
    if (date.getMonthValue() == 9) { // Noncompliant {{Use a "java.time.Month" enum constant instead of this int literal.}}
      //^^^^^^^^^^^^^^^^^^^^^^^^^
      return true;
    }
    if (day.getValue() != 3){// Noncompliant {{Use a "java.time.DayOfWeek" enum constant instead of this int literal.}}
      //^^^^^^^^^^^^^^^^^^^
      return true;
    }
    if (3 == month.getValue()){// Noncompliant {{Use a "java.time.Month" enum constant instead of this int literal.}}
      //^^^^^^^^^^^^^^^^^^^^^
      return true;
    }
    return date.getMonthValue() < 2; // Compliant; this comparison cannot be made using enum constants
  }

  boolean compliantDateManipulation(LocalDate date, DayOfWeek day, Month month) {
    if (date.getMonth() == Month.SEPTEMBER) {
      return true;
    }
    if (month == Month.MARCH) {
      return true;
    }
    return day == DayOfWeek.THURSDAY;
  }


  void quickfixDateCreation() {
    LocalDate date = LocalDate.of(2024, 1, 15); // Noncompliant [[quickfixes=qf1]]
    // fix@qf1 {{Replace with Month.JANUARY.}}
    // edit@qf1 [[sc=41;ec=42]]{{Month.JANUARY}}

    LocalDateTime dateTime = LocalDateTime.of(2024, 10, 15, 1, 20); // Noncompliant [[quickfixes=qf2]]
    // fix@qf2 {{Replace with Month.OCTOBER.}}
    // edit@qf2 [[sc=53;ec=55]]{{Month.OCTOBER}}

    MonthDay monthDay = MonthDay.of(2, 16); // Noncompliant [[quickfixes=qf3]]
    // fix@qf3 {{Replace with Month.FEBRUARY.}}
    // edit@qf3 [[sc=37;ec=38]]{{Month.FEBRUARY}}

    DayOfWeek day = DayOfWeek.of(2); // Noncompliant [[quickfixes=qf4]]
    // fix@qf4 {{Replace with DayOfWeek.TUESDAY.}}
    // edit@qf4 [[sc=21;ec=36]]{{DayOfWeek.TUESDAY}}

    Month month = Month.of(12); // Noncompliant [[quickfixes=qf5]]
    int i = month.getValue();
    // fix@qf5 {{Replace with Month.DECEMBER.}}
    // edit@qf5 [[sc=19;ec=31]]{{Month.DECEMBER}}
  }

  void quickfixDateManipulation(LocalDate date, DayOfWeek day, Month month) {
    if (date.getMonthValue() == 9) { // Noncompliant [[quickfixes=qf6]]
      // fix@qf6 {{Replace with date.getMonth().equals(Month.SEPTEMBER).}}
      // edit@qf6 [[sc=9;ec=34]]{{date.getMonth().equals(Month.SEPTEMBER)}}
    }
    if (day.getValue() != 3) { // Noncompliant [[quickfixes=qf7]]
      // fix@qf7 {{Replace with !day.equals(DayOfWeek.WEDNESDAY).}}
      // edit@qf7 [[sc=9;ec=28]]{{!day.equals(DayOfWeek.WEDNESDAY)}}
    }
    if (3 == month.getValue()) { // Noncompliant [[quickfixes=qf8]]
      // fix@qf8 {{Replace with Month.MARCH.equals(month).}}
      // edit@qf8 [[sc=9;ec=30]]{{Month.MARCH.equals(month)}}
    }
    boolean isSeptember = (LocalDate.now().getMonthValue() != 9); // Noncompliant [[quickfixes=qf9]]
    // fix@qf9 {{Replace with !LocalDate.now().getMonth().equals(Month.SEPTEMBER).}}
    // edit@qf9 [[sc=28;ec=64]]{{!LocalDate.now().getMonth().equals(Month.SEPTEMBER)}}
  }

}
