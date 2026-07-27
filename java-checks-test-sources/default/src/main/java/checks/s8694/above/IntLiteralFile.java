package checks.s8694.above;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.YearMonth;

// 8 out of 9 total usages across the project use int literals (89%) -> above 80% threshold -> issues are raised
public class IntLiteralFile {

  void test(LocalDate date, DayOfWeek day, Month month) {
    LocalDate.of(2024, 1, 15); // Noncompliant {{Use a "java.time.Month" enum constant instead of this int literal.}}
    YearMonth.of(2025, 3); // Noncompliant {{Use a "java.time.Month" enum constant instead of this int literal.}}
    MonthDay.of(2, 16); // Noncompliant {{Use a "java.time.Month" enum constant instead of this int literal.}}
    DayOfWeek.of(2); // Noncompliant {{Use a "java.time.DayOfWeek" enum constant instead of this int literal.}}
    Month.of(10); // Noncompliant {{Use a "java.time.Month" enum constant instead of this int literal.}}
    boolean b1 = date.getMonthValue() == 9; // Noncompliant {{Use a "java.time.Month" enum constant instead of this int literal.}}
    boolean b2 = day.getValue() != 3; // Noncompliant {{Use a "java.time.DayOfWeek" enum constant instead of this int literal.}}
    boolean b3 = 3 == month.getValue(); // Noncompliant {{Use a "java.time.Month" enum constant instead of this int literal.}}
    boolean b4 = date.getMonthValue() < 2; // Compliant; this comparison cannot be made using enum constants
  }

}