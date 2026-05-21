package checks;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

public abstract class SimpleTemporalInstantiationCheckSample {

  Temporal[] method(Instant instant, ZoneId zoneId) {
    return new Temporal[]{

      LocalDate.now(), // Compliant
      LocalDate.from(Instant.now()),  // Noncompliant

      LocalDate.from(instant),  // Compliant
      LocalDate.from(instant.atZone(ZoneId.of("UTC"))), // Compliant
      LocalDate.from(zonedDateTime()), // Compliant

      LocalDate.now(ZoneId.of("UTC")), // Compliant
      LocalDate.from(Instant.now().atZone(ZoneId.of("UTC"))),  // Noncompliant {{Replace "from(TemporalAccessor)" with "now(ZoneId)".}} [[quickfixes=qf1]]
      //        ^^^^
      // @fix@qf1 {{Replace with now(ZoneId.of("UTC"))}}
      // edit@qf1 [[sc=17;ec=61]] {{now(ZoneId.of("UTC"))}}

      LocalDate.now(ZoneId.of("UTC")), // Compliant
      LocalDate.from(ZonedDateTime.now(zoneId)), // Noncompliant [[quickfixes=qf2]]
      //        ^^^^
      // fix@qf2 {{Replace with now(zoneId)}}
      // edit@qf2 [[sc=17;ec=48]] {{now(zoneId)}}

      LocalDate.now(ZoneId.of("UTC")), // Compliant
      LocalDate.from(OffsetDateTime.now(ZoneId.of("Asia/Tokyo"))), // Noncompliant

      LocalTime.now(), // Compliant
      LocalTime.from(Instant.now()),  // Noncompliant

      LocalTime.now(ZoneId.of("UTC")), // Compliant
      LocalTime.from(Instant.now().atZone(ZoneId.of("UTC"))), // Noncompliant

      LocalTime.now(ZoneId.of("UTC")), // Compliant
      LocalTime.from(ZonedDateTime.now(ZoneId.of("UTC"))), // Noncompliant

      LocalTime.now(ZoneId.of("UTC")), // Compliant
      LocalTime.from(OffsetDateTime.now(ZoneId.of("UTC"))), // Noncompliant

      YearMonth.now(), // Compliant
      YearMonth.from(Instant.now()),  // Noncompliant

      YearMonth.now(ZoneId.of("UTC")), // Compliant
      YearMonth.from(Instant.now().atZone(ZoneId.of("UTC"))), // Noncompliant

      YearMonth.now(ZoneId.of("UTC")), // Compliant
      YearMonth.from(ZonedDateTime.now(ZoneId.of("UTC"))), // Noncompliant

      YearMonth.now(ZoneId.of("UTC")), // Compliant
      YearMonth.from(OffsetDateTime.now(ZoneId.of("UTC"))), // Noncompliant

      YearMonth.now(ZoneId.of("UTC")), // Compliant
      YearMonth.from(LocalDate.now(ZoneId.of("UTC"))), // Noncompliant

      Year.now(), // Compliant
      Year.from(Instant.now()),  // Noncompliant

      Year.now(ZoneId.of("UTC")), // Compliant
      Year.from(Instant.now().atZone(ZoneId.of("UTC"))), // Noncompliant

      Year.now(ZoneId.of("UTC")), // Compliant
      Year.from(ZonedDateTime.now(ZoneId.of("UTC"))), // Noncompliant

      Year.now(ZoneId.of("UTC")), // Compliant
      Year.from(OffsetDateTime.now(ZoneId.of("UTC"))), // Noncompliant

      Year.now(ZoneId.of("UTC")), // Compliant
      Year.from(LocalDate.now(ZoneId.of("UTC"))), // Noncompliant

      Year.now(ZoneId.of("UTC")), // Compliant
      Year.from(YearMonth.now(ZoneId.of("UTC"))) // Noncompliant
    };
  }

  public abstract ZonedDateTime zonedDateTime();

}
