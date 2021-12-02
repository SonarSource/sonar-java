package symbolicexecution.checks;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

class DivisionByZeroCheck {
  void foo(int r) {
    int z1 = 0; // flow@foo {{Implies 'z1' is zero.}}
    int z2 = z1; // flow@foo {{Implies 'z2' has the same value as 'z1'.}}
    r = 1 / z2; // Noncompliant [[flows=foo]] {{Make sure "z2" can't be zero before doing this division.}} flow@foo {{Division by zero.}}
  }

  void roo(int r) {
    int z1 = 0;
    int z2 = z1;
    r = z2 / 1; // Compliant
  }

  void boo(int r) {
    r = 1 / '\0'; // Noncompliant [[sc=13;ec=17]] {{Make sure this expression can't be zero before doing this division.}}
  }

  void choo(int r) {
    r = 1 / '4'; // Compliant
  }

  int chaloo(char c, int r) {
    if (c != '0') {
      return r / c; // Compliant - code of char '0' is 48
    }
    return r / c; // Compliant - we know nothing about zero-ness of c
  }

  void too(int r) {
    r = 1 / (int) '\u0000'; // Noncompliant [[sc=13;ec=27]] {{Make sure this expression can't be zero before doing this division.}}
  }

  void moo1(int r) {
    r = 1 / (int) 0b0101_000_01; // Compliant
  }

  void moo2(int r) {
    r = 1 / (int) 0B0101_000_01; // Compliant
  }

  void doo(int r) {
    r = 1 / (int) getDoubleValue(); // Compliant
  }

  double getDoubleValue() {
    return -1;
  }

  void zug(int r) {
    int z1 = 0x0;
    int z2 = 15;
    z2 *= z1;
    r = 1 / z2; // Noncompliant {{Make sure "z2" can't be zero before doing this division.}}
  }

  void zug2(int r) {
    int z1 = 0X0;
    int z2 = 15;
    z2 *= z1;
    r = 1 / z2; // Noncompliant {{Make sure "z2" can't be zero before doing this division.}}
  }

  void zup(int r) {
    int z1 = 0x1;
    int z2 = 15;
    z2 *= z1;
    r = 1 / z2; // Compliant
  }

  void pug(int x, int y, int a) {
    x *= y;
    if (x == 0) {
      int b = a / x; // Noncompliant
    }
  }

  void rug(int r) {
    int z1 = 0;
    int z2 = 15;
    r = 1 / (z2 * z1); // Noncompliant [[sc=13;ec=22]] {{Make sure this expression can't be zero before doing this division.}}
  }

  void tug(int r) {
    int z1 = 0;
    int z2 = 15;
    r = 1 / (z2 + z1); // Compliant
  }

  void mug(int r) {
    int z1 = 0x00;
    int z2 = (int) 0x00L;
    r = 1 / (z2 + z1); // Noncompliant [[sc=13;ec=22]] {{Make sure this expression can't be zero before doing this division.}}
  }

  void mug2(int r) {
    int z1 = 0x00;
    int z2 = (int) 0x00l;
    r = 1 / (z2 + z1); // Noncompliant [[sc=13;ec=22]] {{Make sure this expression can't be zero before doing this division.}}
  }

  int pdf(int p) {
    int r = 0;
    r = -r;
    return p / r; // Noncompliant
  }

  int ptt(int p) {
    int r = 0;
    r++;
    return p / r; // Compliant
  }

  int jac(int p) {
    if (p == 0) {
      return 0;
    }

    if (p < 0) {
      p = -p;
    }

    if (p == 1) {
      return 1;
    }

    int u = 14;
    u %= p; // Compliant
    return 0;
  }

  int car(int s) {
    if (s >= 0) {
    }
    if(s > 0 ) {
      int x = 14 / s;  // Compliant
    }
    return 0;
  }

  int mar(int s) {
    if (s >= 0) {
      int x = 15 / s; // Compliant
    }
    if (s <= 0) {
      int x = 15 / s; // Compliant - FN
    } else {
      int x = 14 / s; // Compliant
    }
    return 0;
  }

  int par(int s) {
    double weight = 0.0;
    if (weight > 0.0) {
      int dx = (int) (s / weight); // Compliant
    }
    return 1;
  }

  void preferredLayoutSize(boolean useBaseline) {
    class Dim {
      int width;
      int height;
    }
    Dim dim = new Dim();
    int maxAscent = 0;
    int maxDescent = 0;
    int width = 14;

    dim.width += width;
    width = maxAscent + maxDescent;
  }

  int getValue() {
    return 0;
  }

  void add(int r) {
    int z1 = 0;
    int z2 = z1 + 15;
    r = 1 / z2; // Compliant
  }

  void alo(int r) {
    int z1 = 0;
    int z2 = z1 * 15;
    r = 1 % z2; // Noncompliant {{Make sure "z2" can't be zero before doing this modulation.}}
  }

  void arg(int r) {
    int z1 = 0;
    int z2 = z1 * 15;
    r = 1 % z2; // Noncompliant {{Make sure "z2" can't be zero before doing this modulation.}}
  }

  void qix(boolean b, int r) {
    int z1 = 0;
    if (b) {
      z1 = 3;
    } else {
      r = 1;
    }
    r = 1 / z1; // Noncompliant {{Make sure "z1" can't be zero before doing this division.}}
  }

  void bar(boolean b, long r) {
    long z1 = 0L;
    if (b) {
      r = 1L;
    } else {
      z1 = 3L;
    }
    r /= z1; // Noncompliant {{Make sure "z1" can't be zero before doing this division.}}
  }

  void bul(boolean b, int r) {
    int z1 = 14;
    if (b) {
      z1 = 0;
    } else {
      z1 = 52;
    }
    r /= z1; // Noncompliant {{Make sure "z1" can't be zero before doing this division.}}
  }

  void zul(int r) {
    if (r == 0) {
      int z1 = 14 / r; // Noncompliant {{Make sure "r" can't be zero before doing this division.}}
    }
    int z2 = 14 / r;
  }

  void tol(int r) {
    if (0 < r) {
      int z1 = 14 % r;
    }
    int z2 = 14 % r; // Compliant - False Negative
  }

  void gol(int r) {
    if (r <= 0) {
      int z1 = 14 / r; // Compliant - False Negative
    }
    int z2 = 14 / r;
  }

  void tul(int r) {
    if (r > 0) {
      int z1 = 14 % r;
    }
    int z2 = 14 % r; // Compliant - False Negative
  }

  void gon(int r) {
    if (0 >= r) {
      int z1 = 14 / r; // Compliant - False Negative
    }
    int z2 = 14 / r;
  }

  void gor(int r) {
    if (r != 0) {
      int z1 = 14 / r;
    }
    int z2 = 14 / r; // Noncompliant {{Make sure "r" can't be zero before doing this division.}}
  }

  void goo(int r) {
    if (!(r != 0)) {
      int z1 = 14 / r; // Noncompliant {{Make sure "r" can't be zero before doing this division.}}
    }
    int z2 = 14 / r; // Compliant
  }

  void gra(boolean b, int r) {
    int z1 = 0;
    if (b) {
      r = 1;
    } else {
      z1 = 3;
    }
    r = 1 % z1; // Noncompliant {{Make sure "z1" can't be zero before doing this modulation.}}
  }

  void gou(boolean b, float r) {
    float z1 = 0.0f;
    if (b) {
      z1 = 3.0f;
    } else {
      r = 1.0f;
    }
    r %= z1; // Noncompliant {{Make sure "z1" can't be zero before doing this modulation.}}
  }

  void gou2(boolean b, float r) {
    float z1 = 0.0F;
    if (b) {
      z1 = 3.0F;
    } else {
      r = 1.0F;
    }
    r %= z1; // Noncompliant {{Make sure "z1" can't be zero before doing this modulation.}}
  }

  void gou3(boolean b, double r) {
    double z1 = 0.0d;
    if (b) {
      z1 = 3.0d;
    } else {
      r = 1.0d;
    }
    r %= z1; // Noncompliant {{Make sure "z1" can't be zero before doing this modulation.}}
  }

  void gou4(boolean b, double r) {
    double z1 = 0.0D;
    if (b) {
      z1 = 3.0D;
    } else {
      r = 1.0D;
    }
    r %= z1; // Noncompliant {{Make sure "z1" can't be zero before doing this modulation.}}
  }

  void woo(boolean b) {
    Long myLong = null;
    if (b) {
      myLong = 0L;
    }
    if (myLong != null) {
      long x = 42 / myLong; // Noncompliant
    }
  }

  void roo(boolean b) {
    Long myLong = 14L;
    if (b) {
      myLong = null;
    }
    if (myLong != null) {
      long x = 42 / myLong; // Compliant
    }
  }

  long avgSize() {
    int count = 0;
    if (count == 0) {
      return -1L;
    } else {
      return 14 / count; // Compliant
    }
  }

  long avgSize2() {
    int count = 0;
    if (count != 0) {
      return 14 / count; // Compliant
    } else {
      return -1L;
    }
  }

  long avgSize3() {
    int count = 0;
    if (count != 0) {
      return -1L;
    } else {
      return 14 / count; // Noncompliant
    }
  }

  long avgSize4(boolean b) {
    int count = 0;
    if (b) {
      count = 14;
    }
    if (count != 0) {
      return 14 / count; // Compliant
    } else {
      return -1L;
    }
  }

  long avgSize5(int[] values) {
    long sum = 0;
    int count = 0;
    for (int value : values) {
      sum += value;
      count++;
    }
    if (count == 0) {
      return -1L;
    } else {
      return sum / count; // Compliant
    }
  }

  double i93(int sum) {
    int count = 0;
    return count == 0 ? Double.NaN : (sum / count); // Compliant
  }

  double fdsf(double x, double y, double a) {
    if (x * 0.0 + y * 0.0 == a) {
      return 14 / a; // Noncompliant
    }
    return 1.2;
  }

  int ifStatement(int x) {
    if (x == 0) {
      return 42/x; // Noncompliant
    } else if (x == 1) {
      return 23/x;
    } else {
      return 13/x;
    }
  }

  int switchStatement1(int x) {
    switch (x) {
      case 0:
        return 42/x; // Noncompliant
      case 1:
        return 23/x; // Compliant
      default:
        return 13/x; // Compliant
    }
  }

  int switchStatement2(int x) {
    switch (x) {
      case 0:
      case 42:
        return 42/x; // Noncompliant
      case 1:
        return 23/x; // Compliant
      default:
        return 13/x; // Compliant
    }
  }

  int switchStatement3(int x) {
    switch (x) {
      case 0:
        return 42;
    }
    return 42/x; // Compliant
  }

  int switchStatement4(int x) {
    switch (x) {
      case 0:
        break;
    }
    return 42/x; // Noncompliant
  }

  int nestedSwitchStatement(int x, int y) {
    switch(x) {
      case 0:
        switch(y) {
          case 0:
            x=1;
            y=1;
        }
      case 42:
        y = 1;
        break;
      default:
        switch (y) {
          case 0:
            y=1;
        }
    }
    return x/y; // Compliant
  }

  int switchExpression1(int x) {
    return switch (x) {
      case 0, 42 -> 42/x; // Noncompliant
      case 1 -> 23/x; // Compliant
      default -> 13/x; // Compliant
    };
  }

  int switchExpression2(int x) {
    return 42 / (switch (x) { case 0 -> x; default -> 42;} + 0); // Noncompliant
  }

  int switchExpression3(int x) {
    return 42 / (switch (x) { case 0 -> 42; default -> x;} + 0); // Compliant
  }

  void dsdf(int a, int b) {
    int c = 0;
    int d = c + b + (a*c);
  }

  long hashSymbol(byte[] buf) {
    long h = 0;
    int s = 0;
    int len = buf.length;
    while (len-- > 0) {
      h = 31*h + (0xFFL & buf[s]);
      s++;
    }
    return h & 0xFFFFFFFFL;
  }

  private void decodeBigInteger(int value) throws Exception {
    long lowBits = 0;
    while (value > 0) {
      int b = value & 1;
      if (b == 0) {
        if (lowBits == 0) {
          // do something
        }
      } else {
        lowBits += b;
      }
    }
    lowBits = -lowBits;
  }
}

class DivisionByZeroCheckAssignment {
  int myField;

  public int calculate(int i) {
    this.myField *= 0;
    return i / myField; // Noncompliant
  }

  public int calculateTwo(int i) {
    myField *= 0;
    return i / this.myField; // Noncompliant
  }
}

class DivisionByZeroCheckConstraintCopy {
  void f(int x) {
    boolean b1 = x < 0 || (x == 0.0 && (1 / x > 0)); // Noncompliant
  }

  void g(int x) {
    boolean b2 = x >= -1 && x == 0 && (1 / x >= 0); // Noncompliant
  }
}

class DivisionByZeroCheckTwoCompoundAssignments {

  double sSum;
  double mSum;

  public void xxx() {
    this.sSum = 0.0d;
    this.mSum = 0.0d;

    double sSumAdd = 1;

    this.sSum += sSumAdd;
    this.mSum += sSumAdd;
  }
}

class DivisionByZeroCheckNonZeroAfterDiv {

  void test(int i, int j) {
    int x = i / j;
    if (j == 0) { // j can't be zero, because previous division was succesfull
      x = i / j; // unreachable
    }
  }

  void transitive(int i, int j, int k) {
    if (j == k) {
      int x = i / j;
      if (k == 0) { // j != 0, k == j
        x = i / k; // unreachable
      }
    }
  }

}

class DivisionByZeroCheckRelationalOperators {
  void h() {
    int x = 1;
    int a = 0;
    if (x >= a) {
      int y = 1 / a; // Noncompliant
    }
  }

  void h2() {
    int x = 1;
    int a = 0;
    if (x > a) {
      int y = 1 / a; // Noncompliant
    }
  }

  void h3() {
    int x = -1;
    int a = 0;
    if (x < a) {
      int y = 1 / a; // Noncompliant
    }
  }

  void h4() {
    int x = -1;
    int a = 0;
    if (x <= a) {
      int y = 1 / a; // Noncompliant
    }
  }

  void f() {
    g(0, 1);
  }

  private int g(int x, int y) {
    if (x > 0) {
      return y / x;  // Compliant
    } else {
      return y;
    }
  }
}

class DivisionByZeroCheckBigIntegerAndDecimal {
  void simpleBigInt(BigInteger r) {
    BigInteger z1 = BigInteger.valueOf(0); // flow@simpleBigInt {{'valueOf()' can return zero.}} flow@simpleBigInt {{Implies 'z1' can be zero.}}
    BigInteger z2 = z1; // flow@simpleBigInt {{Implies 'z2' has the same value as 'z1'.}}
    r = r.divide(z2); // Noncompliant [[flows=simpleBigInt]] {{Make sure "z2" can't be zero before doing this division.}} flow@simpleBigInt {{Division by zero.}}
  }

  void simpleBigDec(BigDecimal r) {
    BigDecimal z1 = BigDecimal.valueOf(0);;
    BigDecimal z2 = z1;
    r = r.divide(z2); // Noncompliant
  }

  void bigMultiply(BigInteger r) {
    BigInteger z1 = BigInteger.valueOf(0);
    BigInteger b1 = BigInteger.valueOf(4);
    BigInteger mult1 = z1.multiply(b1);

    r = r.divide(mult1); // Noncompliant
  }

  void bigMultiply2(BigInteger r) {
    BigInteger z1 = BigInteger.valueOf(0);
    BigInteger b1 = BigInteger.valueOf(4);
    BigInteger mult2 = b1.multiply(z1);

    r = r.divide(mult2); // Noncompliant
  }

  void bigMultiplyChained(BigInteger r) {
    BigInteger z1 = BigInteger.valueOf(0);
    BigInteger b1 = BigInteger.valueOf(4);
    r = r.divide(z1.multiply(b1)); // Noncompliant
  }

  void bigPlus(BigInteger r) {
    BigInteger z1 = BigInteger.valueOf(0);
    BigInteger z2 = BigInteger.valueOf(0);
    BigInteger sum1 = z1.add(z2);

    r = r.divide(sum1); // Noncompliant
  }

  void bigMinus(BigInteger r) {
    BigInteger z1 = BigInteger.valueOf(0);
    BigInteger z2 = BigInteger.valueOf(0);
    BigInteger sum1 = z1.subtract(z2);

    r = r.divide(sum1); // Noncompliant
  }

  void bigRemainder(BigInteger r) {
    BigInteger z1 = BigInteger.valueOf(0);
    BigInteger v1 = BigInteger.valueOf(42);
    BigInteger z2 = z1.multiply(v1);
    r = r.remainder(z2); // Noncompliant
  }

  void simpleDivideAndRemainder(BigInteger r) {
    BigInteger z1 = BigInteger.valueOf(0);
    r.divideAndRemainder(z1); // Noncompliant
  }

  void simpleBigInt(BigInteger r, int value) {
    BigInteger z1 = BigInteger.valueOf(value);
    r = r.divide(z1); // Compliant
  }

  // From Constant
  void simpleBigIntFromZeroConstant(BigInteger r) {
    BigInteger z1 = BigInteger.ZERO;
    r = r.divide(z1); // Noncompliant
  }

  void simpleBigDecFromZeroConstant(BigDecimal r) {
    BigDecimal z1 = BigDecimal.ZERO;
    r = r.divide(z1); // Noncompliant
  }

  void simpleBigIntFromOneConstant(BigInteger r) {
    BigInteger z1 = BigInteger.ONE;
    r = r.divide(z1); // Compliant
  }

  void simpleBigDecFromTenConstant(BigDecimal r) {
    BigDecimal z1 = BigDecimal.TEN;
    r = r.divide(z1); // Compliant
  }

  // Keeping zero constraints
  void keepZeroConstraint(BigInteger r) {
    int i = 0;
    BigInteger z1 = BigInteger.valueOf(i);
    r = r.divide(z1); // Noncompliant
  }

  void keepZeroConstraint(BigDecimal r) {
    double i = 0.0;
    BigDecimal z1 = BigDecimal.valueOf(i);
    r = r.divide(z1); // Noncompliant
  }

  void keepZeroConstraint2(BigInteger r) {
    int i = 0;
    long j = i;
    BigInteger z1 = BigInteger.valueOf(j);
    r = r.divide(z1); // Noncompliant
  }

  // Operations keeping constraints
  void keepToBigInteger(BigInteger r) {
    BigDecimal z1 = BigDecimal.valueOf(0);
    BigInteger z2 = z1.toBigInteger();
    r = r.divide(z2); // Noncompliant
  }

  void keepToBigIntegerExact(BigInteger r) {
    BigDecimal z1 = BigDecimal.valueOf(0);
    BigInteger z2 = z1.toBigIntegerExact();
    r = r.divide(z2); // Noncompliant
  }

  void keepRound(BigDecimal r) {
    MathContext mc = new MathContext(5);
    BigDecimal z1 = BigDecimal.valueOf(0);
    z1 = z1.round(mc);
    r = r.divide(z1); // Noncompliant
  }

  void keepRemainder(BigDecimal r) {
    BigDecimal z1 = BigDecimal.valueOf(0);
    z1 = z1.remainder(r);
    r = r.divide(z1); // Noncompliant
  }

  void keepPow(BigDecimal r) {
    BigDecimal z1 = BigDecimal.valueOf(0);
    z1 = z1.pow(42);
    r = r.divide(z1); // Noncompliant
  }

  void keepLongValueExact(long r) {
    BigInteger z1 = BigInteger.valueOf(0);
    long l1 = z1.longValueExact();
    r = r / l1; // Noncompliant
  }

  void keepLongValue(long r) {
    BigInteger z1 = BigInteger.valueOf(0);
    long l1 = z1.longValue();
    r = r / l1; // Noncompliant
  }

  void keepIntValueExact(int r) {
    BigInteger z1 = BigInteger.valueOf(0);
    int l1 = z1.intValueExact();
    r = r / l1; // Noncompliant
  }

  void keepIntValue(int r) {
    BigInteger z1 = BigInteger.valueOf(0);
    int l1 = z1.intValue();
    r = r / l1; // Noncompliant
  }

  void keepFloatValue(float r) {
    BigInteger z1 = BigInteger.valueOf(0);
    float l1 = z1.floatValue();
    r = r / l1; // Noncompliant
  }

  void keepDoubleValue(double r) {
    BigInteger z1 = BigInteger.valueOf(0);
    double l1 = z1.doubleValue();
    r = r / l1; // Noncompliant
  }

  void keepShortValueExact(short r) {
    BigInteger z1 = BigInteger.valueOf(0);
    short s1 = z1.shortValueExact();
    int res = r / s1; // Noncompliant
  }

  void keepShortValue(short r) {
    BigInteger z1 = BigInteger.valueOf(0);
    short s1 = z1.shortValue();
    int res = r / s1; // Noncompliant
  }

  void keepByteValue(byte r) {
    BigInteger z1 = BigInteger.valueOf(0);
    byte s1 = z1.byteValue();
    int res = r / s1; // Noncompliant
  }

  void keepByteValueExact(byte r) {
    BigInteger z1 = BigInteger.valueOf(0);
    byte s1 = z1.byteValueExact();
    int res = r / s1; // Noncompliant
  }

  void keepAbs(BigInteger r) {
    BigInteger z1 = BigInteger.valueOf(0);
    BigInteger abs = z1.abs();
    r = r.divide(abs); // Noncompliant
  }

  void keepShift(BigInteger r) {
    BigInteger z1 = BigInteger.valueOf(0);
    z1 = z1.shiftLeft(42);
    z1 = z1.shiftRight(42);
    r = r.divide(z1); // Noncompliant
  }
}
