package checks;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;

public class InstantConversionsCheckSample {

  void instantToLocal(Instant instant, ZoneId zone) {
    LocalDate.from(instant); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//                 ^^^^^^^
    LocalTime.from(instant); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//                 ^^^^^^^
    LocalDateTime.from(instant); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//                     ^^^^^^^
    DayOfWeek.from(instant); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//                 ^^^^^^^
    Month.from(instant); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//             ^^^^^^^
    MonthDay.from(instant); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//                ^^^^^^^
    Year.from(instant); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//            ^^^^^^^
    YearMonth.from(instant); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//                 ^^^^^^^

    LocalDate.from(instant.atZone(zone)); // Compliant
    LocalTime.from(instant.atZone(zone)); // Compliant
    LocalDateTime.ofInstant(instant, zone); // Compliant
    Year.from(instant.atZone(zone)); // Compliant
  }

  void localToInstant(LocalDate date, LocalTime time, LocalDateTime dateTime, MonthDay monthDay, Year year, YearMonth yearMonth) {
    Instant.from(date); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//               ^^^^
    Instant.from(time); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//               ^^^^
    Instant.from(dateTime); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//               ^^^^^^^^
    Instant.from(DayOfWeek.MONDAY); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//               ^^^^^^^^^^^^^^^^
    Instant.from(Month.JANUARY); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//               ^^^^^^^^^^^^^
    Instant.from(monthDay); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//               ^^^^^^^^
    Instant.from(year); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//               ^^^^
    Instant.from(yearMonth); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//               ^^^^^^^^^

    Instant.from(date.atStartOfDay(ZoneId.systemDefault())); // Compliant
    date.atStartOfDay(ZoneId.systemDefault()).toInstant(); // Compliant
    date.atStartOfDay().toInstant(ZoneOffset.UTC); // Compliant
  }

  void castsAndOtherTemporalTypes(LocalDate date, Instant instant, TemporalAccessor temporal, OffsetDateTime offsetDateTime, ZonedDateTime zonedDateTime) {
    Instant.from((TemporalAccessor) date); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//               ^^^^^^^^^^^^^^^^^^^^^^^
    LocalDate.from((TemporalAccessor) instant); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//                 ^^^^^^^^^^^^^^^^^^^^^^^^^^
    LocalDateTime.from((instant)); // Noncompliant {{Provide explicit timezone information when converting between local date/time types and "Instant".}}
//                     ^^^^^^^^^

    Instant.from(instant); // Compliant
    Instant.from(temporal); // Compliant
    Instant.from(offsetDateTime); // Compliant
    Instant.from(zonedDateTime); // Compliant
    LocalDate.from(temporal); // Compliant
    LocalDate.from(offsetDateTime); // Compliant
    LocalDate.from(zonedDateTime); // Compliant
  }
}
