package checks;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.YEARS;
import static checks.DurationGetTemporalUnitCheckSample.CustomUnit.CUSTOM_MINUTES;

class DurationGetTemporalUnitCheckSample {

  private static final TemporalUnit STORED_UNIT = ChronoUnit.MINUTES;

  long unsupported(Duration duration) {
    long result = 0;
    result += duration.get(ChronoUnit.MINUTES); // Noncompliant {{"Duration.get()" only supports "SECONDS" and "NANOS"; use dedicated conversion methods instead.}}
//                         ^^^^^^^^^^^^^^^^^^
    result += duration.get(ChronoUnit.MILLIS); // Noncompliant {{"Duration.get()" only supports "SECONDS" and "NANOS"; use dedicated conversion methods instead.}}
//                         ^^^^^^^^^^^^^^^^^
    result += duration.get(MINUTES); // Noncompliant {{"Duration.get()" only supports "SECONDS" and "NANOS"; use dedicated conversion methods instead.}}
//                         ^^^^^^^
    result += duration.get(YEARS); // Noncompliant
    result += duration.get(ChronoUnit.MICROS); // Noncompliant
    result += duration.get(ChronoUnit.HOURS); // Noncompliant
    result += duration.get(ChronoUnit.HALF_DAYS); // Noncompliant
    result += duration.get(ChronoUnit.DAYS); // Noncompliant
    result += duration.get(ChronoUnit.WEEKS); // Noncompliant
    result += duration.get(ChronoUnit.MONTHS); // Noncompliant
    result += duration.get(ChronoUnit.DECADES); // Noncompliant
    result += duration.get(ChronoUnit.CENTURIES); // Noncompliant
    result += duration.get(java.time.temporal.ChronoUnit.MILLENNIA); // Noncompliant
    result += duration.get(ChronoUnit.ERAS); // Noncompliant
    result += duration.get(ChronoUnit.FOREVER); // Noncompliant

    result += duration.get((ChronoUnit.MINUTES)); // Noncompliant
//                         ^^^^^^^^^^^^^^^^^^^^
    return result;
  }

  long supported(Duration duration) {
    long result = 0;
    result += duration.get(ChronoUnit.SECONDS);
    result += duration.get(ChronoUnit.NANOS);
    return result;
  }

  long otherApis(Duration duration) {
    long result = 0;
    result += duration.getSeconds();
    result += duration.getNano();
    result += duration.toMillis();
    result += duration.toMinutes();
    result += duration.toHours();
    result += duration.toDays();
    result += duration.toNanos();
    return result;
  }

  long otherReceiver(CustomDuration custom) {
    return custom.get(ChronoUnit.MINUTES);
  }

  long indirectAndDynamic(Duration duration, TemporalUnit unit, boolean condition) {
    TemporalUnit localUnit = ChronoUnit.MINUTES;
    long result = 0;
    result += duration.get(localUnit);
    result += duration.get(STORED_UNIT);
    result += duration.get(unit);
    result += duration.get(condition ? ChronoUnit.MINUTES : ChronoUnit.SECONDS);
    return result;
  }

  long customUnits(Duration duration) {
    long result = 0;
    result += duration.get(CustomUnit.CUSTOM_MINUTES);
    result += duration.get(CUSTOM_MINUTES);
    return result;
  }

  private static class CustomDuration {
    long get(TemporalUnit unit) {
      return 0;
    }
  }

  enum CustomUnit implements TemporalUnit {
    CUSTOM_MINUTES;

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
