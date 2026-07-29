package checks.s8694.caching;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;

public class ClassWithIssues {

  void test(LocalDate date, DayOfWeek day, Month month) {
    LocalDate.of(2024, 1, 15); // Noncompliant {{Use a "java.time.Month" enum constant instead of this int literal.}}
    boolean b1 = date.getMonthValue() == 9; // Noncompliant {{Use a "java.time.Month" enum constant instead of this int literal.}}
    boolean b2 = day.getValue() != 3; // Noncompliant {{Use a "java.time.DayOfWeek" enum constant instead of this int literal.}}
    boolean b3 = date.getMonthValue() < 2; // Compliant; this comparison cannot be made using enum constants
  }

}
