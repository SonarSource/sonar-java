package checks;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;

import static java.time.temporal.ChronoUnit.YEARS;
import static checks.UnsupportedChronoUnitWithInstantCheckSample.CustomUnit.MONTHS;

class UnsupportedChronoUnitWithInstantCheckSample {

  private static final TemporalUnit STORED_UNIT = ChronoUnit.MONTHS;

  void unsupported(Instant instant, Instant end) {
    instant.plus(1, ChronoUnit.WEEKS); // Noncompliant {{"WEEKS" is unsupported by Instant and causes an UnsupportedTemporalTypeException.}}
//                  ^^^^^^^^^^^^^^^^
    instant.plus(1, ChronoUnit.MONTHS); // Noncompliant {{"MONTHS" is unsupported by Instant and causes an UnsupportedTemporalTypeException.}}
//                  ^^^^^^^^^^^^^^^^^
    instant.plus(1, YEARS); // Noncompliant {{"YEARS" is unsupported by Instant and causes an UnsupportedTemporalTypeException.}}
//                  ^^^^^
    instant.plus(1, ChronoUnit.DECADES); // Noncompliant
    instant.plus(1, ChronoUnit.CENTURIES); // Noncompliant
    instant.plus(1, java.time.temporal.ChronoUnit.MILLENNIA); // Noncompliant
    instant.plus(1, ChronoUnit.ERAS); // Noncompliant
    instant.plus(1, ChronoUnit.FOREVER); // Noncompliant

    instant.minus(1, ChronoUnit.WEEKS); // Noncompliant
    instant.minus(1, ChronoUnit.MONTHS); // Noncompliant
    instant.minus(1, ChronoUnit.YEARS); // Noncompliant
    instant.minus(1, ChronoUnit.DECADES); // Noncompliant
    instant.minus(1, ChronoUnit.CENTURIES); // Noncompliant
    instant.minus(1, ChronoUnit.MILLENNIA); // Noncompliant
    instant.minus(1, ChronoUnit.ERAS); // Noncompliant
    instant.minus(1, ChronoUnit.FOREVER); // Noncompliant

    instant.until(end, ChronoUnit.WEEKS); // Noncompliant
    instant.until(end, (ChronoUnit.MONTHS)); // Noncompliant
//                     ^^^^^^^^^^^^^^^^^^^
    instant.until(end, ChronoUnit.YEARS); // Noncompliant
    instant.until(end, ChronoUnit.DECADES); // Noncompliant
    instant.until(end, ChronoUnit.CENTURIES); // Noncompliant
    instant.until(end, ChronoUnit.MILLENNIA); // Noncompliant
    instant.until(end, ChronoUnit.ERAS); // Noncompliant
    instant.until(end, ChronoUnit.FOREVER); // Noncompliant
  }

  void supported(Instant instant, Instant end) {
    instant.plus(1, ChronoUnit.NANOS);
    instant.plus(1, ChronoUnit.MICROS);
    instant.plus(1, ChronoUnit.MILLIS);
    instant.plus(1, ChronoUnit.SECONDS);
    instant.plus(1, ChronoUnit.MINUTES);
    instant.plus(1, ChronoUnit.HOURS);
    instant.plus(1, ChronoUnit.HALF_DAYS);
    instant.plus(1, ChronoUnit.DAYS);

    instant.minus(1, ChronoUnit.NANOS);
    instant.minus(1, ChronoUnit.MICROS);
    instant.minus(1, ChronoUnit.MILLIS);
    instant.minus(1, ChronoUnit.SECONDS);
    instant.minus(1, ChronoUnit.MINUTES);
    instant.minus(1, ChronoUnit.HOURS);
    instant.minus(1, ChronoUnit.HALF_DAYS);
    instant.minus(1, ChronoUnit.DAYS);

    instant.until(end, ChronoUnit.NANOS);
    instant.until(end, ChronoUnit.MICROS);
    instant.until(end, ChronoUnit.MILLIS);
    instant.until(end, ChronoUnit.SECONDS);
    instant.until(end, ChronoUnit.MINUTES);
    instant.until(end, ChronoUnit.HOURS);
    instant.until(end, ChronoUnit.HALF_DAYS);
    instant.until(end, ChronoUnit.DAYS);
  }

  void excludedApis(Instant instant) {
    instant.plus(Duration.ofDays(1));
    instant.minus(Duration.ofDays(1));
    instant.truncatedTo(ChronoUnit.WEEKS);
  }

  void otherReceiver(ZonedDateTime dateTime, ZonedDateTime end) {
    dateTime.plus(1, ChronoUnit.MONTHS);
    dateTime.minus(1, ChronoUnit.YEARS);
    dateTime.until(end, ChronoUnit.WEEKS);
  }

  void indirectAndDynamic(Instant instant, Instant end, TemporalUnit unit, boolean condition) {
    TemporalUnit localUnit = ChronoUnit.YEARS;
    instant.plus(1, localUnit);
    instant.minus(1, STORED_UNIT);
    instant.until(end, unit);
    instant.plus(1, condition ? ChronoUnit.MONTHS : ChronoUnit.DAYS);
  }

  void customUnits(Instant instant, Instant end) {
    instant.plus(1, CustomUnit.MONTHS);
    instant.minus(1, MONTHS);
    instant.until(end, CustomUnit.MONTHS);
  }

  enum CustomUnit implements TemporalUnit {
    MONTHS;

    @Override
    public Duration getDuration() {
      return Duration.ZERO;
    }

    @Override
    public boolean isDurationEstimated() {
      return false;
    }

    @Override
    public boolean isDateBased() {
      return false;
    }

    @Override
    public boolean isTimeBased() {
      return true;
    }

    @Override
    public boolean isSupportedBy(Temporal temporal) {
      return true;
    }

    @Override
    public <R extends Temporal> R addTo(R temporal, long amount) {
      return temporal;
    }

    @Override
    public long between(Temporal first, Temporal second) {
      return 0;
    }
  }
}
