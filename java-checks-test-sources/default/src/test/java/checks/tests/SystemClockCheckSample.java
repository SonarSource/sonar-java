package checks.tests;

import java.time.Clock;
import java.time.Instant;
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
    Instant now = Instant.now(); // Noncompliant {{Replace this use of the system clock with a fixed clock.}}
//                ^^^^^^^^^^^^^
    Instant now2 = Instant.now(); // Noncompliant {{Replace this use of the system clock with a fixed clock.}}
//                 ^^^^^^^^^^^^^
    assertTrue(now.isBefore(now2));
  }

  @Test
  void testInjectSystemClock() {
    SecurityService securityService = new SecurityService(Clock.systemDefaultZone()); // Noncompliant {{Replace this use of the system clock with a fixed clock.}}
//                                                        ^^^^^^^^^^^^^^^^^^^^^^^^^
    assertTrue(securityService.isTokenValid(Instant.now(Clock.system(ZoneId.systemDefault())).minusSeconds(60))); // Noncompliant {{Replace this use of the system clock with a fixed clock.}}
//                                                      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  @Test
  void testFixedClock() {
    Instant start = Instant.now(Clock.fixed(Instant.parse("2026-05-07T10:00:00Z"), ZoneOffset.UTC)); // Compliant
    Instant later = start.plus(1, ChronoUnit.MINUTES);
    assertTrue(later.isBefore(start));
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
