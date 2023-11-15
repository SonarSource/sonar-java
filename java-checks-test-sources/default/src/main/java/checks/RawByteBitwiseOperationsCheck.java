package checks;

class RawByteBitwiseOperationsCheck {
  byte b = 100;

  void foo() {
    RawByteBitwiseOperationsCheck a = new RawByteBitwiseOperationsCheck();
    int value = 1;
    long longValue = 1L;
    byte b = 100;

    value = (value << 8) + b;            // Noncompliant [[sc=28;ec=29]] {{Prevent "int" promotion by adding "& 0xff" to this expression.}}
    value = readByte() + (value << 8);   // Noncompliant [[sc=13;ec=23]] {{Prevent "int" promotion by adding "& 0xff" to this expression.}}
    value = b | (value << 8);            // Noncompliant [[sc=13;ec=14]] {{Prevent "int" promotion by adding "& 0xff" to this expression.}}
    value = (value << 8) | readByte();   // Noncompliant [[sc=28;ec=38]] {{Prevent "int" promotion by adding "& 0xff" to this expression.}}

    value = (value << 8) + (b & 0xff);          // Compliant
    value = (0xff & readByte()) + (value << 8); // Compliant
    value = (0xff & b) | (value << 8);          // Compliant
    value = (value << 8) | (readByte() & 0xff); // Compliant
    value = (value << 8) | (readByte() | 0xaa); // Compliant

    longValue = (longValue << 8) + a.b;          // Noncompliant [[sc=36;ec=39]] {{Prevent "int" promotion by adding "& 0xff" to this expression.}}
    longValue = a.readByte() | (longValue << 8); // Noncompliant [[sc=17;ec=29]] {{Prevent "int" promotion by adding "& 0xff" to this expression.}}

    value += b; // Compliant
    value += readByte(); // Compliant
    value = value & b; // Compliant

    boolean bar = bar() & bar();
  }

  byte readByte() { return 0;}

  boolean bar() { return true; }
}
