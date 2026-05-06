package checks;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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

  void nowWithParameters(Clock clock, ZoneId zoneId) {
    LocalDateTime dateTime = LocalDateTime.now(ZoneId.systemDefault());
    LocalDate date = LocalDate.now(clock);
    LocalTime time = LocalTime.now(ZoneId.of("Europe/Zurich"));
    MonthDay monthDay = MonthDay.now(ZoneId.of("UTC"));
    OffsetDateTime offsetDateTime = OffsetDateTime.now(zoneId);
  }

  void nowOnInstant() {
    Instant instant = Instant.now(); // Compliant, instant doesn't need a timezone
  }

  Year nowOnOtherPatterns() {
    System.out.println(OffsetTime.now()); // Noncompliant {{Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.}}
    //                            ^^^^^
    String formatted = ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE); // Noncompliant {{Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.}}
    //                               ^^^^^
    return Year.now(); // Noncompliant {{Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.}}
    //          ^^^^^
  }

}
