package checks.s8694.above;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.YearMonth;

// 8 out of 9 total usages across the project use int literals (89%) -> above 80% threshold -> considered code-style -> no issues raised
public class IntLiteralFile {

  void test(LocalDate date, DayOfWeek day, Month month) {
    LocalDate.of(2024, 1, 15); // Compliant - above threshold, int literals are the dominant style
    YearMonth.of(2025, 3); // Compliant - above threshold, int literals are the dominant style
    MonthDay.of(2, 16); // Compliant - above threshold, int literals are the dominant style
    DayOfWeek.of(2); // Compliant - above threshold, int literals are the dominant style
    Month.of(10); // Compliant - above threshold, int literals are the dominant style
    boolean b1 = date.getMonthValue() == 9; // Compliant - above threshold, int literals are the dominant style
    boolean b2 = day.getValue() != 3; // Compliant - above threshold, int literals are the dominant style
    boolean b3 = 3 == month.getValue(); // Compliant - above threshold, int literals are the dominant style
    boolean b4 = date.getMonthValue() < 2; // Compliant; this comparison cannot be made using enum constants
  }

}
