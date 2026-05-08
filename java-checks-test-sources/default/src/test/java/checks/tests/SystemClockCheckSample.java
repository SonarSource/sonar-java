package checks.tests;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class SystemClockCheckSample {

  @Mock
  private Clock clock;

  static class SecurityService {
    private final Clock clock;

    public SecurityService(Clock clock) {
      this.clock = clock;
    }

    public boolean isTokenValid(Instant issuedAt) {
      return issuedAt.isAfter(Instant.now(clock).minus(1, ChronoUnit.HOURS));
    }
  }

  @Test
  void testSystemClockInstants() {
    Instant now = Instant.now(); // Noncompliant {{Do not use the system clock in tests.}}
//                ^^^^^^^^^^^^^
    Instant now2 = Instant.now(Clock.system(ZoneId.systemDefault())); // Noncompliant {{Do not use the system clock in tests.}}
//                             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    assertTrue(now.isBefore(now2));
  }

  void testLocalDateTimeTypes() {
    LocalDateTime dateTime1 = LocalDateTime.now(Clock.systemUTC()); // Noncompliant {{Do not use the system clock in tests.}}
//                                              ^^^^^^^^^^^^^^^^^
    LocalDateTime dateTime2 = LocalDateTime.now(); // Noncompliant {{Do not use the system clock in tests.}}
//                            ^^^^^^^^^^^^^^^^^^^
    assertTrue(dateTime1.isBefore(dateTime2));
  }

  @Test
  void testInjectSystemClock() {
    SecurityService securityService = new SecurityService(Clock.systemDefaultZone()); // Noncompliant {{Do not use the system clock in tests.}}
//                                                        ^^^^^^^^^^^^^^^^^^^^^^^^^
    assertTrue(securityService.isTokenValid(Instant.now(Clock.system(ZoneId.systemDefault())).minusSeconds(60))); // Noncompliant {{Do not use the system clock in tests.}}
//                                                      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  @Test
  void testFixedClock() {
    Instant start = Instant.now(Clock.fixed(Instant.parse("2026-05-07T10:00:00Z"), ZoneOffset.UTC)); // Compliant
    Instant later = start.plus(1, ChronoUnit.MINUTES);
    assertTrue(start.isBefore(later));
  }

  @Test
  void testInjectFixedClock() {
    Instant fixedPoint = Instant.parse("2026-05-07T10:00:00Z"); // Compliant
    when(clock.instant()).thenReturn(fixedPoint);
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);

    SecurityService service = new SecurityService(clock);
    Instant issuedAt = Instant.parse("2026-05-07T09:30:00Z"); // Compliant
    assertTrue(service.isTokenValid(issuedAt));
  }

}
