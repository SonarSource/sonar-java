package checks.s8694.above;

import java.time.LocalDate;
import java.time.Month;

// 8 out of 9 total usages across the project use int literals (89%) -> above 80% threshold -> considered code-style -> no issues raised
public class EnumFile {

  void test() {
    LocalDate.of(2024, Month.JUNE, 1); // Compliant; uses enum constant, counted toward total
  }

}
