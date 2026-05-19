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
    OffsetDateTime offsetDateTime = OffsetDateTime.of(2025, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC); // Noncompliant
    //                                                      ^
    ZonedDateTime zdt = ZonedDateTime.of(2026, 12, 21, 3, 33, 33, 4, ZoneId.of("Europe/Paris")); // Noncompliant
    //                                         ^^
    YearMonth ym = YearMonth.of(2025, 3); // Noncompliant
    //                                ^
    MonthDay md = MonthDay.of(2, 16); // Noncompliant
    //                        ^
    MonthDay monthDay = MonthDay.of(+2, 16); // Noncompliant
    LocalDate ld = LocalDate.of(2024, -1, 15); // Noncompliant
    DayOfWeek day = DayOfWeek.of(2); // Noncompliant {{Use a "java.time.DayOfWeek" enum constant instead of this int literal.}}
    Month month = Month.of(10); // Noncompliant {{Use a "java.time.Month" enum constant instead of this int literal.}}
    DayOfWeek parenthesized = DayOfWeek.of((-3)); // Noncompliant {{Use a "java.time.DayOfWeek" enum constant instead of this int literal.}}
    DayOfWeek parenthesizedOutside = DayOfWeek.of(-(3)); // Noncompliant {{Use a "java.time.DayOfWeek" enum constant instead of this int literal.}}
  }

  void compliantDateCreation(int monthNumber) {
    LocalDate date = LocalDate.of(2024, Month.JANUARY, 15);
    LocalDateTime dateTime = LocalDateTime.of(2024, Month.DECEMBER, 25, 10, 30);
    YearMonth yearMonth = YearMonth.of(2024, Month.JUNE);
    LocalDateTime dateTimeWithVariable = LocalDateTime.of(2024, monthNumber, 25, 10, 30);
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


}
