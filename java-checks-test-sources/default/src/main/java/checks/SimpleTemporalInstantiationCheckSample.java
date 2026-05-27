package checks;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

public class SimpleTemporalInstantiationCheckSample {

  static Temporal[] method(Instant instant, ZoneId zoneId, Clock clock) {
    return new Temporal[]{

      LocalDate.now(), // Compliant
      LocalDate.from(Instant.now()),  // Noncompliant
      LocalDate.from(LocalDate.now()),  // Noncompliant [[quickfixes=qf0]]
      //        ^^^^
      // fix@qf0 {{Replace with now()}}
      // edit@qf0 [[sc=17;ec=38]] {{now()}}
      LocalDate.from(LocalTime.now()),  // Noncompliant
      LocalDate.from(YearMonth.now()),  // Noncompliant
      LocalDate.from(Year.now()),  // Noncompliant


      LocalDate.from(Instant.now(clock)),  // Noncompliant
      LocalDate.from(ZonedDateTime.now(clock)),  // Noncompliant
      LocalDate.from(OffsetDateTime.now(clock)),  // Noncompliant
      LocalDate.from(clock.instant()),  // Compliant, limitation of the rule
      LocalDate.now(clock),  // Compliant
      LocalTime.now(clock),  // Compliant
      YearMonth.now(clock),  // Compliant
      Year.now(clock),  // Compliant

      LocalDate.from(LocalDate.now(clock)),  // Noncompliant [[quickfixes=qf1]]
      //        ^^^^
      // fix@qf1 {{Replace with now(clock)}}
      // edit@qf1 [[sc=17;ec=43]] {{now(clock)}}
      LocalDate.from(LocalTime.now(clock)),  // Noncompliant
      LocalDate.from(YearMonth.now(clock)),  // Noncompliant
      LocalDate.from(Year.now(clock)),  // Noncompliant

      LocalDate.from(instant),  // Compliant
      LocalDate.from(instant.atZone(ZoneId.of("UTC"))), // Compliant
      LocalDate.from(zonedDateTime()), // Compliant

      LocalDate.now(), // Compliant
      LocalDate.from(LocalDate.now()), // Noncompliant {{Replace with "now()".}}

      LocalDate.now(ZoneId.of("UTC")), // Compliant
      LocalDate.from(Instant.now().atZone(ZoneId.of("UTC"))), // Noncompliant {{Replace with "now(ZoneId.of("UTC"))".}} [[quickfixes=qf2]]
      //        ^^^^
      // @fix@qf2 {{Replace with now(ZoneId.of("UTC"))}}
      // edit@qf2 [[sc=17;ec=61]] {{now(ZoneId.of("UTC"))}}

      LocalDate.now(ZoneId.of("UTC")), // Compliant
      LocalDate.from(ZonedDateTime.now(zoneId)), // Noncompliant {{Replace with "now(zoneId)".}} [[quickfixes=qf3]]
      //        ^^^^
      // fix@qf3 {{Replace with now(zoneId)}}
      // edit@qf3 [[sc=17;ec=48]] {{now(zoneId)}}

      LocalDate.now(ZoneId.of("UTC")), // Compliant
      LocalDate.from(OffsetDateTime.now(ZoneId.of("Asia/Tokyo"))), // Noncompliant

      LocalTime.now(), // Compliant
      LocalTime.from(Instant.now()),  // Noncompliant

      LocalTime.now(), // Compliant
      LocalTime.from(LocalTime.now()),  // Noncompliant

      LocalTime.now(zoneId), // Compliant
      LocalTime.from(LocalTime.now(zoneId)),  // Noncompliant

      LocalTime.now(ZoneId.of("UTC")), // Compliant
      LocalTime.from(Instant.now().atZone(ZoneId.of("UTC"))), // Noncompliant

      LocalTime.now(ZoneId.of("UTC")), // Compliant
      LocalTime.from(ZonedDateTime.now(ZoneId.of("UTC"))), // Noncompliant

      LocalTime.now(ZoneId.of("UTC")), // Compliant
      LocalTime.from(OffsetDateTime.now(ZoneId.of("UTC"))), // Noncompliant

      YearMonth.now(), // Compliant
      YearMonth.from(Instant.now()),  // Noncompliant

      YearMonth.now(), // Compliant
      YearMonth.from(YearMonth.now()),  // Noncompliant

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

      Year.now(), // Compliant
      Year.from(Year.now()),  // Noncompliant

      Year.now(zoneId), // Compliant
      Year.from(Year.now(zoneId)),  // Noncompliant

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

  public static ZonedDateTime zonedDateTime() {
    return ZonedDateTime.now(ZoneId.of("UTC"));
  }

}
