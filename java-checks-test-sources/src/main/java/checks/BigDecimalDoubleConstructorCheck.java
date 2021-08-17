package checks;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

class BigDecimalDoubleConstructorCheck {
  MathContext mc;
  BigDecimal bd1 = new BigDecimal("1");
  BigDecimal bd1_1 = new BigDecimal("2", mc);
  BigDecimal bd1_2 = new BigDecimal(new BigInteger("10"), mc);
  BigDecimal bd1_3 = new BigDecimal(1);
  BigDecimal bd2 = new BigDecimal(2.0); // Noncompliant [[sc=20;ec=39;quickfixes=qf1]] {{Use "BigDecimal.valueOf" instead.}}
  // fix@qf1 {{Replace with BigDecimal.valueOf}}
  // edit@qf1 [[sc=20;ec=34]] {{BigDecimal.valueOf}}
  BigDecimal bd4 = new BigDecimal(2.0, mc); // Noncompliant [[sc=20;ec=43;quickfixes=!]] {{Use "BigDecimal.valueOf" instead.}}
  BigDecimal bd5 = new BigDecimal(2.0f); // Noncompliant {{Use "BigDecimal.valueOf" instead.}}
  BigDecimal bd6 = new BigDecimal(2.0f, mc); // Noncompliant {{Use "BigDecimal.valueOf" instead.}}
  BigDecimal bd3 = BigDecimal.valueOf(2.0);

  Double d = 0.1;
  BigDecimal bd7 = new BigDecimal(d); // Noncompliant
  BigDecimal bd8 = new BigDecimal((Double) 0.1); // Noncompliant

  double d2 = 0.1;
  BigDecimal bd7_2 = new BigDecimal(d2); // Noncompliant
  BigDecimal bd8_2 = new BigDecimal((double) 0.1); // Noncompliant

  Float f = 0.1f;
  BigDecimal bd9 = new BigDecimal(f); // Noncompliant
  BigDecimal bd10 = new BigDecimal((Float) 0.1f); // Noncompliant

  float f2 = 0.1f;
  BigDecimal bd9_2 = new BigDecimal(f2); // Noncompliant
  BigDecimal bd10_2 = new BigDecimal((float) 0.1); // Noncompliant [[sc=23;ec=50;quickfixes=qf2]]
  // fix@qf2 {{Replace with BigDecimal.valueOf}}
  // edit@qf2 [[sc=23;ec=37]] {{BigDecimal.valueOf}}

  Object myDoubleObject = 23.5d;
  BigDecimal bd11 = new BigDecimal((Double) myDoubleObject); // Noncompliant

  Object myIntObject = 12;
  BigDecimal bd12 = new BigDecimal((Integer) myIntObject);
  BigDecimal bd13 = new BigDecimal((Integer) 1);
}
