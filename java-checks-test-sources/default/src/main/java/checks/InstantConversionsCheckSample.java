package checks;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;

public class InstantConversionsCheckSample {

  private Instant instant;
  private TemporalAccessor temporalAccessor;
  private LocalDate date;
  private LocalTime time;
  private LocalDateTime dateTime;
  private MonthDay monthDay;
  private Year year;
  private YearMonth yearMonth;
  private ZonedDateTime zonedDateTime;
  private OffsetDateTime offsetDateTime;
  private OffsetTime offsetTime;
  private ZoneId zoneId;
  private ZoneOffset zoneOffset;

  void instant() {
    Instant.from(date); // Noncompliant {{Provide explicit timezone information when converting from a local date/time type to an instant.}}
//               ^^^^
    Instant.from(time); // Noncompliant {{Provide explicit timezone information when converting from a local date/time type to an instant.}}
//               ^^^^
    Instant.from(dateTime); // Noncompliant {{Provide explicit timezone information when converting from a local date/time type to an instant.}}
//               ^^^^^^^^
    Instant.from(DayOfWeek.MONDAY); // Noncompliant {{Provide explicit timezone information when converting from a local date/time type to an instant.}}
//               ^^^^^^^^^^^^^^^^
    Instant.from(Month.JANUARY); // Noncompliant {{Provide explicit timezone information when converting from a local date/time type to an instant.}}
//               ^^^^^^^^^^^^^
    Instant.from(monthDay); // Noncompliant {{Provide explicit timezone information when converting from a local date/time type to an instant.}}
//               ^^^^^^^^
    Instant.from(year); // Noncompliant {{Provide explicit timezone information when converting from a local date/time type to an instant.}}
//               ^^^^
    Instant.from(yearMonth); // Noncompliant {{Provide explicit timezone information when converting from a local date/time type to an instant.}}
//               ^^^^^^^^^
    Instant.from((TemporalAccessor) date); // Noncompliant {{Provide explicit timezone information when converting from a local date/time type to an instant.}}
//               ^^^^^^^^^^^^^^^^^^^^^^^

    Instant.from(zonedDateTime); // Compliant
    Instant.from(temporalAccessor); // Compliant
    Instant.from(offsetDateTime); // Compliant
    Instant.from(zonedDateTime); // Compliant
    Instant.from(date.atStartOfDay(ZoneId.systemDefault())); // Compliant
    date.atStartOfDay(zoneId).toInstant(); // Compliant
    date.atStartOfDay().toInstant(ZoneOffset.UTC); // Compliant
  }

  void localDateTimeTypes() {
    LocalDate.from(instant); // Noncompliant {{Provide explicit timezone information when converting from an instant to a local date/time type.}}
//                 ^^^^^^^
    LocalTime.from(instant); // Noncompliant {{Provide explicit timezone information when converting from an instant to a local date/time type.}}
//                 ^^^^^^^
    LocalDateTime.from(instant); // Noncompliant {{Provide explicit timezone information when converting from an instant to a local date/time type.}}
//                     ^^^^^^^
    DayOfWeek.from(instant); // Noncompliant {{Provide explicit timezone information when converting from an instant to a local date/time type.}}
//                 ^^^^^^^
    Month.from(instant); // Noncompliant {{Provide explicit timezone information when converting from an instant to a local date/time type.}}
//             ^^^^^^^
    MonthDay.from(instant); // Noncompliant {{Provide explicit timezone information when converting from an instant to a local date/time type.}}
//                ^^^^^^^
    Year.from(instant); // Noncompliant {{Provide explicit timezone information when converting from an instant to a local date/time type.}}
//            ^^^^^^^
    YearMonth.from(instant); // Noncompliant {{Provide explicit timezone information when converting from an instant to a local date/time type.}}
//                 ^^^^^^^
    LocalDate.from((TemporalAccessor) instant); // Noncompliant {{Provide explicit timezone information when converting from an instant to a local date/time type.}}
//                 ^^^^^^^^^^^^^^^^^^^^^^^^^^

    LocalDateTime.from(ZonedDateTime.now()); // Compliant
    LocalDate.from(instant.atZone(zoneId)); // Compliant
    LocalTime.from(instant.atZone(zoneId)); // Compliant
    LocalDateTime.ofInstant(instant, zoneId); // Compliant
    LocalDateTime.from(instant.atOffset(zoneOffset)); // Compliant
    Year.from(instant.atZone(zoneId)); // Compliant
  }

  void zoneAwareDateTimeTypes() {
    ZonedDateTime.from(instant); // Noncompliant {{Provide explicit timezone information when converting from a local date/time type or an instant to a timezone aware type.}}
//                     ^^^^^^^
    ZonedDateTime.from(dateTime); // Noncompliant {{Provide explicit timezone information when converting from a local date/time type or an instant to a timezone aware type.}}
//                     ^^^^^^^^
    OffsetDateTime.from(instant); // Noncompliant {{Provide explicit timezone information when converting from a local date/time type or an instant to a timezone aware type.}}
//                      ^^^^^^^
    OffsetTime.from(instant); // Noncompliant {{Provide explicit timezone information when converting from a local date/time type or an instant to a timezone aware type.}}
//                  ^^^^^^^
    ZonedDateTime.from(((TemporalAccessor) instant)); // Noncompliant {{Provide explicit timezone information when converting from a local date/time type or an instant to a timezone aware type.}}
//                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    ZonedDateTime.ofInstant(instant, zoneId); // Compliant
    ZonedDateTime.from(dateTime.atZone(zoneId)); // Compliant
    OffsetDateTime.from(instant.atOffset(ZoneOffset.UTC)); // Compliant
    OffsetTime.from(instant.atOffset(ZoneOffset.UTC)); // Compliant
  }

}
