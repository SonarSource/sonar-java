package checks;

import com.google.common.base.Supplier;
import java.math.BigDecimal;
import java.math.BigInteger;

class StringPrimitiveConstructorCheck {

  void bar(char[] chars, String str) {
    String empty = new String(); // Noncompliant [[sc=24;ec=30;quickfixes=qf1]] {{Remove this "String" constructor}}
    // fix@qf1 {{Replace with ""}}
    // edit@qf1 [[sc=20;ec=32]] {{""}}

    Supplier<String> methodReference = String::new; // method references are always compliant

    String nonempty = new String("Hello world"); // Noncompliant [[sc=27;ec=33;quickfixes=qf2]] {{Remove this "String" constructor}}
    // fix@qf2 {{Remove "new String"}}
    // edit@qf2 [[sc=23;ec=34]] {{}}
    // edit@qf2 [[sc=47;ec=48]] {{}}

    nonempty = new String(chars);
    Double myDouble = new Double(1.1); // Noncompliant [[sc=27;ec=33;quickfixes=qf3]] {{Remove this "Double" constructor}}
    // fix@qf3 {{Replace with Double.valueOf}}
    // edit@qf3 [[sc=23;ec=34]] {{Double.valueOf(}}

    Integer integer = new Integer(1); // Noncompliant [[sc=27;ec=34;quickfixes=qf4]] {{Remove this "Integer" constructor}}
    // fix@qf4 {{Replace with Integer.valueOf}}
    // edit@qf4 [[sc=23;ec=35]] {{Integer.valueOf(}}

    Boolean bool = new Boolean(true); // Noncompliant [[sc=24;ec=31;quickfixes=qf5]] {{Remove this "Boolean" constructor}}
    // fix@qf5 {{Replace with Boolean.valueOf}}
    // edit@qf5 [[sc=20;ec=32]] {{Boolean.valueOf(}}

    Character myChar = new Character('c'); // Noncompliant [[sc=28;ec=37;quickfixes=qf6]] {{Remove this "Character" constructor}}
    // fix@qf6 {{Replace with Character.valueOf}}
    // edit@qf6 [[sc=24;ec=38]] {{Character.valueOf(}}

    Long myLong = new Long(1L); // Noncompliant [[sc=23;ec=27;quickfixes=qf7]] {{Remove this "Long" constructor}}
    // fix@qf7 {{Replace with Long.valueOf}}
    // edit@qf7 [[sc=19;ec=28]] {{Long.valueOf(}}

    byte b = 0;
    Byte myByte = new Byte(b); // Noncompliant [[sc=23;ec=27;quickfixes=qf8]] {{Remove this "Byte" constructor}}
    // fix@qf8 {{Replace with Byte.valueOf}}
    // edit@qf8 [[sc=19;ec=28]] {{Byte.valueOf(}}

    Short myShort = new Short((short) 0); // Noncompliant [[sc=25;ec=30;quickfixes=qf9]] {{Remove this "Short" constructor}}
    // fix@qf9 {{Replace with Short.valueOf}}
    // edit@qf9 [[sc=21;ec=31]] {{Short.valueOf(}}

    Float myFloat = new Float(1.0f); // Noncompliant [[sc=25;ec=30;quickfixes=qf10]] {{Remove this "Float" constructor}}
    // fix@qf10 {{Replace with Float.valueOf}}
    // edit@qf10 [[sc=21;ec=31]] {{Float.valueOf(}}

    BigInteger bigInteger0 = new BigInteger(str);
    BigInteger bigInteger1 = new BigInteger("1"); // Noncompliant [[sc=34;ec=44;quickfixes=qf11]] {{Remove this "BigInteger" constructor}}
    // fix@qf11 {{Replace with BigInteger.valueOf}}
    // edit@qf11 [[sc=30;ec=46]] {{BigInteger.valueOf(}}
    // edit@qf11 [[sc=47;ec=49]] {{L)}}

    BigInteger bigInteger2 = new BigInteger("9223372036854775807"); // Noncompliant
    BigInteger bigInteger3 = new BigInteger("9223372036854775808");
    BigInteger bigInteger4 = new BigInteger("-9223372036854775808"); // Noncompliant
    BigInteger bigInteger5 = new BigInteger("-9223372036854775809");
    BigInteger bigInteger6 = new BigInteger("error");
    BigDecimal doubleBigDecimal = new BigDecimal(1.1);
    BigDecimal stringBigDecimal = new BigDecimal("1.1");

    int value = new String("Hello " + "world").hashCode(); // Noncompliant [[sc=21;ec=27;quickfixes=qf_expression]] {{Remove this "String" constructor}}
    // fix@qf_expression {{Remove "new String"}}
    // edit@qf_expression [[sc=17;ec=27]] {{}}

    BigInteger bigDecimalWithABody = new BigInteger("1") { // Compliant, can not be replaced by BigInteger.valueOf(1)
      @Override
      public String toString() {
        return "'" + super.toString() + "'";
      }
    };
  }

  void foo() {
    String empty = "";
    String nonempty = "Hello world";
    Double myDouble = Double.valueOf(1.1);
    Integer integer = Integer.valueOf(1);
    Boolean bool = Boolean.valueOf(true);
    BigInteger bigInteger1 = BigInteger.valueOf(1);
    BigInteger bigInteger2 = BigInteger.valueOf(9223372036854775807L);
    BigInteger bigInteger3 = BigInteger.valueOf(-9223372036854775808L);
    BigDecimal doubleBigDecimal = BigDecimal.valueOf(1.1);
  }
}
