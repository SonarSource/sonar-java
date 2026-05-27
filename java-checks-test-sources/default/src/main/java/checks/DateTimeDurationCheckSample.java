package checks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

class DateTimeDurationCheckSample {

  private static final ZoneId UTC = ZoneId.of("UTC");

  void nonCompliantExamples(LocalDateTime localDateTime1, LocalDateTime localDateTime2) {
    Duration duration = Duration.between(LocalDateTime.now(), LocalDateTime.now().plusDays(1)); // Noncompliant {{Convert these arguments to time zone-aware types before computing a duration between them.}}
//                                       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Duration duration2 = Duration.between(LocalDateTime.now().atZone(UTC), LocalDateTime.now().plusDays(1)); // Noncompliant {{Convert this argument to a time zone-aware type before computing a duration on it.}}
//                                                                         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    long nanos = ChronoUnit.NANOS.between(LocalDateTime.now(), LocalDateTime.now().plusNanos(1)); // Noncompliant {{Convert these arguments to time zone-aware types before computing a duration between them.}}
//                                        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    long nanos2 = ChronoUnit.NANOS.between(LocalDateTime.now(), LocalDateTime.now().plusNanos(1).atZone(UTC)); // Noncompliant {{Convert this argument to a time zone-aware type before computing a duration on it.}}
//                                         ^^^^^^^^^^^^^^^^^^^
    long micros = ChronoUnit.MICROS.between(localDateTime1, localDateTime2); // Noncompliant {{Convert these arguments to time zone-aware types before computing a duration between them.}}
//                                          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    long millis = ChronoUnit.MILLIS.between(localDateTime1, localDateTime2); // Noncompliant {{Convert these arguments to time zone-aware types before computing a duration between them.}}
//                                          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), LocalDateTime.now().plusSeconds(5)); // Noncompliant {{Convert these arguments to time zone-aware types before computing a duration between them.}}
//                                            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    long minutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), LocalDateTime.now().plusMinutes(5)); // Noncompliant {{Convert these arguments to time zone-aware types before computing a duration between them.}}
//                                            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    long hours = ChronoUnit.HOURS.between(localDateTime1, localDateTime2); // Noncompliant {{Convert these arguments to time zone-aware types before computing a duration between them.}}
//                                        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    long halfDays = ChronoUnit.HALF_DAYS.between(localDateTime1, localDateTime2); // Noncompliant {{Convert these arguments to time zone-aware types before computing a duration between them.}}
//                                               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    long days = ChronoUnit.DAYS.between(localDateTime1, localDateTime2); // Noncompliant {{Convert these arguments to time zone-aware types before computing a duration between them.}}
//                                      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    long days2 = ChronoUnit.DAYS.between(localDateTime1, localDateTime2.atZone(UTC)); // Noncompliant {{Convert this argument to a time zone-aware type before computing a duration on it.}}
//                                       ^^^^^^^^^^^^^^
  }

  void compliantExamples(LocalDateTime localDateTime1, LocalDateTime localDateTime2) {
    Duration duration = Duration.between(LocalTime.now(), LocalTime.now().plusHours(1)); // Compliant: computing a duration between two local time objects without a date means we don't care about time zones
    long weeks = ChronoUnit.WEEKS.between(localDateTime1, localDateTime2); // Compliant: the number of weeks between two local date time objects is insensitive to time zones or DST
    long months = ChronoUnit.MONTHS.between(localDateTime1, localDateTime2); // Compliant: the number of months between two local date time objects is insensitive to time zones or DST
    long years = ChronoUnit.YEARS.between(localDateTime1, localDateTime2); // Compliant: the number of years between two local date time objects is insensitive to time zones or DST
    long decades = ChronoUnit.DECADES.between(localDateTime1, localDateTime2); // Compliant: the number of decades between two local date time objects is insensitive to time zones or DST
    long centuries = ChronoUnit.CENTURIES.between(localDateTime1, localDateTime2); // Compliant: the number of centuries between two local date time objects is insensitive to time zones or DST
    long millenia = ChronoUnit.MILLENNIA.between(localDateTime1, localDateTime2); // Compliant: the number of millenia between two local date time objects is insensitive to time zones or DST
  }

}
