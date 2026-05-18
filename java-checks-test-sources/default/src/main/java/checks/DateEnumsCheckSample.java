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
    LocalDate date = LocalDate.of(2024, 1, 15); // Noncompliant {{Use a Month enum instead of this numeric value}}
    //                                             ^
    LocalDateTime dateTime = LocalDateTime.of(2024, 10, 15, 1, 20); // Noncompliant {{Use a Month enum instead of this numeric value}}
    //                                                         ^^
    OffsetDateTime offsetDateTime = OffsetDateTime.of(2025, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC); // Noncompliant {{Use a Month enum instead of this numeric value}}
    //                                                                 ^
    ZonedDateTime zdt = ZonedDateTime.of(2026, 12, 21, 3, 33, 33, 4, ZoneId.of("Europe/Paris")); // Noncompliant {{Use a Month enum instead of this numeric value}}
    //                                                    ^^
    YearMonth ym = YearMonth.of(2025, 3); // Noncompliant
    //                                           ^
    MonthDay md = MonthDay.of(2, 16); // Noncompliant
    //                              ^
    DayOfWeek friday = DayOfWeek.of(5); // Noncompliant
    //                                        ^
  }

  void compliantDateCreation(int dayNumber) {
    LocalDate date = LocalDate.of(2024, Month.JANUARY, 15);
    LocalDateTime dateTime = LocalDateTime.of(2024, Month.DECEMBER, 25, 10, 30);
    YearMonth yearMonth = YearMonth.of(2024, Month.JUNE);
    DayOfWeek day = DayOfWeek.of(dayNumber); // Compliant (int value is provided by a variable)
  }

  boolean noncompliantDateManipulation(LocalDate date, DayOfWeek day) {
    if (date.getMonthValue() == 9) { // Noncompliant
      //^^^^^^^^^^^^^^^^^^^^^^^^^
      return true;
    }
    if (day.getValue() == 3){// Noncompliant
      //^^^^^^^^^^^^^^^^^^^
      return true;
    }
    return date.getMonthValue() < 2; // Compliant
  }

  boolean compliantDateManipulation(LocalDate date, DayOfWeek day) {
    if (date.getMonth() == Month.SEPTEMBER) {
      return true;
    }
    return day == DayOfWeek.THURSDAY;
  }


}
