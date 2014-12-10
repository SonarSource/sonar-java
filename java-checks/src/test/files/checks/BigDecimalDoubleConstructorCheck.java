import java.math.BigDecimal;
import java.math.MathContext;

class A {
  MathContext mc;
  BigDecimal bd1 = new BigDecimal("1");
  BigDecimal bd2 = new BigDecimal(2.0); //Non-compliant
  BigDecimal bd2 = new BigDecimal(2.0, mc); //Non-compliant
  BigDecimal bd2 = new BigDecimal(2.0f); //Non-compliant
  BigDecimal bd2 = new BigDecimal(2.0f, mc); //Non-compliant
  BigDecimal bd3 = BigDecimal.valueOf(2.0);



}