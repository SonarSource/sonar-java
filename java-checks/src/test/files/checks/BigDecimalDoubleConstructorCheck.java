import java.math.BigDecimal;
import java.math.MathContext;

class A {
  MathContext mc;
  BigDecimal bd1 = new BigDecimal("1");
  BigDecimal bd2 = new BigDecimal(2.0); // Noncompliant {{Use "BigDecimal.valueOf" instead.}}
  BigDecimal bd2 = new BigDecimal(2.0, mc); // Noncompliant {{Use "BigDecimal.valueOf" instead.}}
  BigDecimal bd2 = new BigDecimal(2.0f); // Noncompliant {{Use "BigDecimal.valueOf" instead.}}
  BigDecimal bd2 = new BigDecimal(2.0f, mc); // Noncompliant {{Use "BigDecimal.valueOf" instead.}}
  BigDecimal bd3 = BigDecimal.valueOf(2.0);



}