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
  BigDecimal bd4 = new BigDecimal(2.0, mc); // Noncompliant [[sc=20;ec=43;quickfixes=qf1_2]] {{Use "new BigDecimal(String, MathContext)" instead.}}
  // fix@qf1_2 {{Replace with BigDecimal("2.0",}}
  // edit@qf1_2 [[sc=35;ec=38]] {{"2.0"}}
  BigDecimal bd43 = new BigDecimal(123.0, mc); // Noncompliant [[sc=21;ec=46;quickfixes=qf1_3]] {{Use "new BigDecimal(String, MathContext)" instead.}}
  // fix@qf1_3 {{Replace with BigDecimal("123.0",}}
  // edit@qf1_3 [[sc=36;ec=41]] {{"123.0"}}

  BigDecimal bd5 = new BigDecimal(2.0f); // Noncompliant {{Use "BigDecimal.valueOf" instead.}}
  BigDecimal bd6 = new BigDecimal(2.0f, mc); // Noncompliant [[sc=20;ec=44;quickfixes=qf1_4]] {{Use "new BigDecimal(String, MathContext)" instead.}}
  // fix@qf1_4 {{Replace with BigDecimal("2.0",}}
  // edit@qf1_4 [[sc=35;ec=39]] {{"2.0"}}

  BigDecimal bd6_2 = new BigDecimal(2.0d, mc); // Noncompliant [[sc=22;ec=46;quickfixes=qf1_5]] {{Use "new BigDecimal(String, MathContext)" instead.}}
  // fix@qf1_5 {{Replace with BigDecimal("2.0",}}
  // edit@qf1_5 [[sc=37;ec=41]] {{"2.0"}}

  BigDecimal bd6_3 = new BigDecimal(2.0F, mc); // Noncompliant [[sc=22;ec=46;quickfixes=qf1_6]] {{Use "new BigDecimal(String, MathContext)" instead.}}
  // fix@qf1_6 {{Replace with BigDecimal("2.0",}}
  // edit@qf1_6 [[sc=37;ec=41]] {{"2.0"}}

  BigDecimal bd6_4 = new BigDecimal(2.0D, mc); // Noncompliant [[sc=22;ec=46;quickfixes=qf1_7]] {{Use "new BigDecimal(String, MathContext)" instead.}}
  // fix@qf1_7 {{Replace with BigDecimal("2.0",}}
  // edit@qf1_7 [[sc=37;ec=41]] {{"2.0"}}

  BigDecimal bd3 = BigDecimal.valueOf(2.0);

  Double d = 0.1;
  BigDecimal bd7 = new BigDecimal(d); // Noncompliant {{Use "BigDecimal.valueOf" instead.}}
  BigDecimal bd8 = new BigDecimal((Double) 0.1); // Noncompliant {{Use "BigDecimal.valueOf" instead.}}
  BigDecimal bd8_3 = new BigDecimal(d, mc); // Noncompliant [[sc=22;ec=43;quickfixes=!]] {{Use "new BigDecimal(String, MathContext)" instead.}}

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
