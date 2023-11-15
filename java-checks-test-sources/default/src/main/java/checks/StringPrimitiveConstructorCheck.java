package checks;

import java.math.BigDecimal;
import java.math.BigInteger;

class StringPrimitiveConstructorCheck {

  void bar(char[] chars, String str) {
    String empty = new String(); // Noncompliant [[sc=24;ec=30]] {{Remove this "String" constructor}}
    String nonempty = new String("Hello world"); // Noncompliant
    nonempty = new String(chars);
    Double myDouble = new Double(1.1); // Noncompliant [[sc=27;ec=33]] {{Remove this "Double" constructor}}
    Integer integer = new Integer(1); // Noncompliant [[sc=27;ec=34]] {{Remove this "Integer" constructor}}
    Boolean bool = new Boolean(true); // Noncompliant [[sc=24;ec=31]] {{Remove this "Boolean" constructor}}
    Character myChar = new Character('c'); // Noncompliant [[sc=28;ec=37]] {{Remove this "Character" constructor}}
    Long myLong = new Long(1L); // Noncompliant [[sc=23;ec=27]] {{Remove this "Long" constructor}}
    byte b = 0;
    Byte myByte = new Byte(b); // Noncompliant [[sc=23;ec=27]] {{Remove this "Byte" constructor}}
    Short myShort = new Short((short) 0); // Noncompliant [[sc=25;ec=30]] {{Remove this "Short" constructor}}
    Float myFloat = new Float(1.0f); // Noncompliant [[sc=25;ec=30]] {{Remove this "Float" constructor}}
    BigInteger bigInteger0 = new BigInteger(str);
    BigInteger bigInteger1 = new BigInteger("1"); // Noncompliant {{Remove this "BigInteger" constructor}}
    BigInteger bigInteger2 = new BigInteger("9223372036854775807"); // Noncompliant
    BigInteger bigInteger3 = new BigInteger("9223372036854775808");
    BigInteger bigInteger4 = new BigInteger("-9223372036854775808"); // Noncompliant
    BigInteger bigInteger5 = new BigInteger("-9223372036854775809");
    BigInteger bigInteger6 = new BigInteger("error");
    BigDecimal doubleBigDecimal = new BigDecimal(1.1);
    BigDecimal stringBigDecimal = new BigDecimal("1.1");

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
    Short myShort = (short) 0;
  }
}

class QuickFixes {

  String empty = new String(); // Noncompliant [[sc=22;ec=28;quickfixes=qf1]]
  // fix@qf1 {{Replace this "String" constructor with an empty string ""}}
  // edit@qf1 [[sc=18;ec=30]] {{""}}

  String nonempty = new String("Hello world"); // Noncompliant [[sc=25;ec=31;quickfixes=qf2]]
  // fix@qf2 {{Replace this "String" constructor with the string literal passed as parameter}}
  // edit@qf2 [[sc=21;ec=46]] {{"Hello world"}}

  Double myDouble = new Double(1.1); // Noncompliant [[sc=25;ec=31;quickfixes=qf3]]
  // fix@qf3 {{Replace this "Double" constructor with the double literal passed as parameter}}
  // edit@qf3 [[sc=21;ec=36]] {{1.1}}

  Integer integer = new Integer(1); // Noncompliant [[sc=25;ec=32;quickfixes=qf4]]
  // fix@qf4 {{Replace this "Integer" constructor with the int literal passed as parameter}}
  // edit@qf4 [[sc=21;ec=35]] {{1}}

  Boolean bool = new Boolean(true); // Noncompliant [[sc=22;ec=29;quickfixes=qf5]]
  // fix@qf5 {{Replace this "Boolean" constructor with the boolean literal passed as parameter}}
  // edit@qf5 [[sc=18;ec=35]] {{true}}

  Character myChar = new Character('c'); // Noncompliant [[sc=26;ec=35;quickfixes=qf6]]
  // fix@qf6 {{Replace this "Character" constructor with the char literal passed as parameter}}
  // edit@qf6 [[sc=22;ec=40]] {{'c'}}

  Long myLong = new Long(1L); // Noncompliant [[sc=21;ec=25;quickfixes=qf7]]
  // fix@qf7 {{Replace this "Long" constructor with the long literal passed as parameter}}
  // edit@qf7 [[sc=17;ec=29]] {{1L}}

  byte b = 0;
  Byte myByte = new Byte(b); // Noncompliant [[sc=21;ec=25;quickfixes=qf8]]
  // fix@qf8 {{Replace this "Byte" constructor with the byte literal passed as parameter}}
  // edit@qf8 [[sc=17;ec=28]] {{b}}

  Short myShort = new Short((short) 0); // Noncompliant [[sc=23;ec=28;quickfixes=qf9]]
  // fix@qf9 {{Replace this "Short" constructor with the short literal passed as parameter}}
  // edit@qf9 [[sc=19;ec=39]] {{(short) 0}}

  Float myFloat = new Float(1.0f); // Noncompliant [[sc=23;ec=28;quickfixes=qf10]]
  // fix@qf10 {{Replace this "Float" constructor with the float literal passed as parameter}}
  // edit@qf10 [[sc=19;ec=34]] {{1.0f}}

  BigInteger bigInteger1 = new BigInteger("1"); // Noncompliant [[sc=32;ec=42;quickfixes=qf11]]
  // fix@qf11 {{Replace this "BigInteger" constructor with "BigInteger.valueOf()" static method}}
  // edit@qf11 [[sc=28;ec=47]] {{BigInteger.valueOf(1L)}}

  BigInteger bigInteger2 = new BigInteger("9223372036854775807"); // Noncompliant [[sc=32;ec=42;quickfixes=qf12]]
  // fix@qf12 {{Replace this "BigInteger" constructor with "BigInteger.valueOf()" static method}}
  // edit@qf12 [[sc=28;ec=65]] {{BigInteger.valueOf(9223372036854775807L)}}
}
