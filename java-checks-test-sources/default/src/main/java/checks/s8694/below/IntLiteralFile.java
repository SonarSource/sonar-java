package checks.s8694.below;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;

// 3 out of 7 total usages across the project use int literals (43%) -> below 80% threshold -> no issues are raised
public class IntLiteralFile {

  void test(LocalDate date, DayOfWeek day, Month month) {
    LocalDate.of(2024, 1, 15); // Compliant - below threshold, no issues raised
    boolean b1 = date.getMonthValue() == 9; // Compliant - below threshold, no issues raised
    boolean b2 = day.getValue() != 3; // Compliant - below threshold, no issues raised
    boolean b3 = date.getMonthValue() < 2; // Compliant; this comparison cannot be made using enum constants
  }

}