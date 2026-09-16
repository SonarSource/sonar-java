/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package checks;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

class DurationTimeUnitAgreementCheckSample {

  private enum MyCustomEnum {
    SECONDS,
    MILLISECONDS
  }

  static class CustomTimeout {
    CustomTimeout(long timeout, TimeUnit unit) {}
  }

  void compliantCases(Future<String> f, Duration d, Instant i) throws Exception {
    f.get(42L, TimeUnit.SECONDS);
    f.get(d.toNanos(), TimeUnit.NANOSECONDS);
    f.get(d.toMillis(), TimeUnit.MILLISECONDS);
    f.get(d.toSeconds(), TimeUnit.SECONDS);
    f.get(d.getSeconds(), TimeUnit.SECONDS);
    f.get(d.toMinutes(), TimeUnit.MINUTES);
    f.get(d.toHours(), TimeUnit.HOURS);
    f.get(d.toDays(), TimeUnit.DAYS);
    f.get(TimeUnit.SECONDS.convert(d), TimeUnit.SECONDS);

    f.get(d.toNanos(), NANOSECONDS);
    f.get(d.toMillis(), MILLISECONDS);
    f.get(d.toSeconds(), SECONDS);
    f.get(d.getSeconds(), SECONDS);
    f.get(d.toMinutes(), MINUTES);
    f.get(d.toHours(), HOURS);
    f.get(d.toDays(), DAYS);

    f.get((d.toMillis()), (TimeUnit.MILLISECONDS));

    TimeUnit unit = TimeUnit.SECONDS;
    f.get(d.toMillis(), unit);
    f.get((long) d.toMillis(), TimeUnit.SECONDS);
    f.get(d.toMillis() * 1000, TimeUnit.SECONDS);
    f.get(i.toEpochMilli(), TimeUnit.SECONDS);
    customMethod(d.toMillis(), MyCustomEnum.SECONDS);

    new CustomTimeout(d.toMillis(), TimeUnit.MILLISECONDS);
  }

  void noncompliantCases(Future<String> f, Duration d) throws Exception {
    f.get(d.toMillis(), TimeUnit.SECONDS); // Noncompliant {{Change this TimeUnit to "MILLISECONDS" or convert the duration to seconds.}}
    f.get(d.toNanos(), TimeUnit.MILLISECONDS); // Noncompliant {{Change this TimeUnit to "NANOSECONDS" or convert the duration to milliseconds.}}
    f.get(d.toSeconds(), TimeUnit.MINUTES); // Noncompliant {{Change this TimeUnit to "SECONDS" or convert the duration to minutes.}}
    f.get(d.getSeconds(), TimeUnit.MINUTES); // Noncompliant {{Change this TimeUnit to "SECONDS" or convert the duration to minutes.}}
    f.get(d.toMinutes(), TimeUnit.SECONDS); // Noncompliant {{Change this TimeUnit to "MINUTES" or convert the duration to seconds.}}
    f.get(d.toHours(), TimeUnit.DAYS); // Noncompliant {{Change this TimeUnit to "HOURS" or convert the duration to days.}}
    f.get(d.toDays(), TimeUnit.HOURS); // Noncompliant {{Change this TimeUnit to "DAYS" or convert the duration to hours.}}
    f.get(TimeUnit.SECONDS.convert(d), TimeUnit.MINUTES); // Noncompliant {{Change this TimeUnit to "SECONDS" or convert the duration to minutes.}}

    f.get(d.toMillis(), SECONDS); // Noncompliant {{Change this TimeUnit to "MILLISECONDS" or convert the duration to seconds.}}
    f.get(d.toNanos(), MILLISECONDS); // Noncompliant {{Change this TimeUnit to "NANOSECONDS" or convert the duration to milliseconds.}}

    new CustomTimeout(d.toMillis(), TimeUnit.SECONDS); // Noncompliant {{Change this TimeUnit to "MILLISECONDS" or convert the duration to seconds.}}
  }

  void customMethod(long timeout, MyCustomEnum unit) {}
}
