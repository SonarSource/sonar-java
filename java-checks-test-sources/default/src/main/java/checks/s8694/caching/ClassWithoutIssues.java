package checks.s8694.caching;

import java.time.*;

public class ClassWithoutIssues {
  void test() {
    LocalDate.of(2024, Month.JUNE, 1); // Compliant
    YearMonth.of(2024, Month.MARCH); // Compliant
    LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0); // Compliant
    MonthDay.of(Month.FEBRUARY, 14); // Compliant
  }
}
