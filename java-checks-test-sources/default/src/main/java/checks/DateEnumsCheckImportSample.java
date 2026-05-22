package checks;

import java.time.LocalDate;

public class DateEnumsCheckImportSample {
  LocalDate date = LocalDate.of(2024, 1, 15);// Noncompliant [[quickfixes=qf1]]
  // fix@qf1 {{Replace with Month.JANUARY.}}
  // edit@qf1 [[sl=6;sc=39;el=6;ec=40]]{{Month.JANUARY}}
  // edit@qf1 [[sl=3;sc=28;el=3;ec=28]]{{\nimport java.time.Month;}}

}

