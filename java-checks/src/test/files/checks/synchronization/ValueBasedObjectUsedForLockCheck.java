import java.time.Clock;
import java.time.LocalDateTime;
import java.time.chrono.HijrahDate;
import java.time.chrono.JapaneseDate;
import java.time.chrono.MinguoDate;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

class A {
  void foo() {
    HijrahDate hijrahDate = HijrahDate.now();
    synchronized (hijrahDate) { } // Noncompliant {{Synchronize on a non-value-based object; synchronizing on a "HijrahDate" could lead to contention. (sonar.java.source not set. Assuming 8 or greater.)}}
    JapaneseDate japaneseDate = JapaneseDate.now();
    synchronized (japaneseDate) { } // Noncompliant
    MinguoDate minguoDate = MinguoDate.now();
    synchronized (minguoDate) { } // Noncompliant
    ThaiBuddhistDate thaiBuddhistDate = ThaiBuddhistDate.now();
    synchronized (thaiBuddhistDate) { } // Noncompliant

    LocalDateTime localDateTime = LocalDateTime.now();
    synchronized (localDateTime) { } // Noncompliant

    Optional<Object> optionalEmpty = Optional.empty();
    synchronized (optionalEmpty) { } // Noncompliant {{Synchronize on a non-value-based object; synchronizing on a "Optional" could lead to contention. (sonar.java.source not set. Assuming 8 or greater.)}}
    OptionalDouble optionalDouble = OptionalDouble.empty();
    synchronized (optionalDouble) { } // Noncompliant
    OptionalLong optionalLong = OptionalLong.empty();
    synchronized (optionalLong) { } // Noncompliant
    OptionalInt optionalInt = OptionalInt.empty();
    synchronized (optionalInt) { } // Noncompliant

    Clock clock = Clock.systemUTC();
    synchronized (clock) { } // Compliant
    Object object = new Object();
    synchronized (object) { } // Compliant
    DateTimeFormatterBuilder dateTimeFormatter = new DateTimeFormatterBuilder();
    synchronized (dateTimeFormatter) { } // Compliant - not part of 'java.time' package but subpackage 'java.time.format'

    synchronized (unknownObject) { } // Compliant
  }
}
