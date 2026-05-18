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
    LocalDate date = LocalDate.of(2024, 1, 15); // Noncompliant {{Use `java.time.Month` enum instead of this int literal}}
    //                                  ^
    LocalDateTime dateTime = LocalDateTime.of(2024, 10, 15, 1, 20); // Noncompliant {{Use `java.time.Month` enum instead of this int literal}}
    //                                              ^^
    OffsetDateTime offsetDateTime = OffsetDateTime.of(2025, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC); // Noncompliant {{Use `java.time.Month` enum instead of this int literal}}
    //                                                      ^
    ZonedDateTime zdt = ZonedDateTime.of(2026, 12, 21, 3, 33, 33, 4, ZoneId.of("Europe/Paris")); // Noncompliant {{Use `java.time.Month` enum instead of this int literal}}
    //                                         ^^
    YearMonth ym = YearMonth.of(2025, 3); // Noncompliant
    //                                ^
    MonthDay md = MonthDay.of(2, 16); // Noncompliant
    //                        ^
    MonthDay monthDay = MonthDay.of(+2, 16); // Noncompliant
    DayOfWeek day = DayOfWeek.of(2); // Noncompliant {{Use `java.time.DayOfWeek` enum instead of this int literal}}
    Month month = Month.of(10); // Noncompliant {{Use `java.time.Month` enum instead of this int literal}}

  }

  void compliantDateCreation(int monthNumber) {
    LocalDate date = LocalDate.of(2024, Month.JANUARY, 15);
    LocalDateTime dateTime = LocalDateTime.of(2024, Month.DECEMBER, 25, 10, 30);
    YearMonth yearMonth = YearMonth.of(2024, Month.JUNE);
    LocalDateTime dateTimeWithVariable = LocalDateTime.of(2024, monthNumber, 25, 10, 30);
  }

  boolean noncompliantDateManipulation(LocalDate date, DayOfWeek day, Month month) {
    if (date.getMonthValue() == 9) { // Noncompliant
      //^^^^^^^^^^^^^^^^^^^^^^^^^
      return true;
    }
    if (day.getValue() != 3){// Noncompliant
      //^^^^^^^^^^^^^^^^^^^
      return true;
    }
    if (3 == month.getValue()){// Noncompliant
      //^^^^^^^^^^^^^^^^^^^^^
      return true;
    }
    return date.getMonthValue() < 2; // Compliant
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
