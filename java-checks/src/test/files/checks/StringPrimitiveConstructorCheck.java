import java.math.BigDecimal;
import java.math.BigInteger;

class A {

  void bar(char[] chars, String str) {
    String empty = new String(); // Noncompliant [[sc=24;ec=30]] {{Remove this "String" constructor}}
    String nonempty = new String("Hello world"); // Noncompliant
    nonempty = new String(chars);
    Double myDouble = new Double(1.1); // Noncompliant [[sc=27;ec=33]] {{Remove this "Double" constructor}}
    Integer integer = new Integer(1); // Noncompliant [[sc=27;ec=34]] {{Remove this "Integer" constructor}}
    Boolean bool = new Boolean(true); // Noncompliant [[sc=24;ec=31]] {{Remove this "Boolean" constructor}}
    Character myChar = new Character('c'); // Noncompliant [[sc=28;ec=37]] {{Remove this "Character" constructor}}
    Long myLong = new Long(1L); // Noncompliant [[sc=23;ec=27]] {{Remove this "Long" constructor}}
    Byte myByte = new Byte(b); // Noncompliant [[sc=23;ec=27]] {{Remove this "Byte" constructor}}
    Short myShort = new Short((short) 0); // Noncompliant [[sc=25;ec=30]] {{Remove this "Short" constructor}}
    Float myFloat = new Float(1.0f); // Noncompliant [[sc=25;ec=30]] {{Remove this "Float" constructor}}
    byte b = 0;
    BigInteger bigInteger0 = new BigInteger(str);
    BigInteger bigInteger1 = new BigInteger("1"); // Noncompliant {{Remove this "BigInteger" constructor}}
    BigInteger bigInteger2 = new BigInteger("9223372036854775807"); // Noncompliant
    BigInteger bigInteger3 = new BigInteger("9223372036854775808");
    BigInteger bigInteger4 = new BigInteger("-9223372036854775808"); // Noncompliant
    BigInteger bigInteger5 = new BigInteger("-9223372036854775809");
    BigInteger bigInteger6 = new BigInteger("error");
    BigDecimal doubleBigDecimal = new BigDecimal(1.1);
    BigDecimal stringBigDecimal = new BigDecimal("1.1");
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
