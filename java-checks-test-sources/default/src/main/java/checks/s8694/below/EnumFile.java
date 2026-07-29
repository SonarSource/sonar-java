package checks.s8694.below;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.YearMonth;

// 3 out of 7 total usages across the project use int literals (43%) -> below 80% threshold -> issues are raised
public class EnumFile {

  void test() {
    LocalDate.of(2024, Month.JUNE, 1); // Compliant
    YearMonth.of(2024, Month.MARCH); // Compliant
    LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0); // Compliant
    MonthDay.of(Month.FEBRUARY, 14); // Compliant
  }

}
