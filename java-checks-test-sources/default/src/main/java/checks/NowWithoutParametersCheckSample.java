package checks;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;

public class NowWithoutParametersCheckSample {

  void nowWithoutParameters() {
    LocalDateTime dateTime = LocalDateTime.now(); // Noncompliant {{Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.}}
    //                                     ^^^^^
    LocalDate date = LocalDate.now(); // Noncompliant {{Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.}}
    //                         ^^^^^
    LocalTime time = LocalTime.now(); // Noncompliant {{Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.}}
    //                         ^^^^^
    MonthDay monthDay = MonthDay.now(); // Noncompliant {{Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.}}
    //                           ^^^^^
    OffsetDateTime offsetDateTime = OffsetDateTime.now(); // Noncompliant {{Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.}}
    //                                             ^^^^^
    OffsetTime offsetTime = OffsetTime.now(); // Noncompliant {{Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.}}
    //                                 ^^^^^
    Year year = Year.now(); // Noncompliant {{Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.}}
    //               ^^^^^
    YearMonth yearMonth = YearMonth.now(); // Noncompliant {{Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.}}
    //                              ^^^^^
    ZonedDateTime zonedDateTime = ZonedDateTime.now(); // Noncompliant {{Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.}}
    //                                          ^^^^^

  }

}
