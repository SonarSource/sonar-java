package checks;

class UselessMathematicalComparisonCheckSample {

  static final int CONSTANT_200 = 200;

  void byteComparisons(byte b) {
    if (b > 200) {} // Noncompliant {{Remove this comparison; it will always return false.}}
//      ^^^^^^^
    if (b > 127) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (b >= 128) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (b < -200) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (b < -128) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (b <= -129) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (b == 200) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (b == -200) {} // Noncompliant {{Remove this comparison; it will always return false.}}

    if (b < 200) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (b <= 127) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (b >= -128) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (b >= -200) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (b > -129) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (b != 200) {} // Noncompliant {{Remove this comparison; it will always return true.}}

    if (b > 100) {} // Compliant - 100 within byte range
    if (b < -100) {} // Compliant
    if (b == 0) {} // Compliant
    if (b <= 126) {} // Compliant - 126 < max, not always true
  }

  void shortComparisons(short s) {
    if (s > 40000) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (s >= 32768) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (s < -32769) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (s <= -32769) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (s == 40000) {} // Noncompliant {{Remove this comparison; it will always return false.}}

    if (s < 32768) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (s <= 32767) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (s >= -32768) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (s != 40000) {} // Noncompliant {{Remove this comparison; it will always return true.}}

    if (s > 30000) {} // Compliant - within range
    if (s < -30000) {} // Compliant
  }

  void charComparisons(char c) {
    if (c < 0) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (c > 65535) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (c >= 65536) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (c < -1) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (c == -1) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (c == 65536) {} // Noncompliant {{Remove this comparison; it will always return false.}}

    if (c >= 0) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (c <= 65535) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (c != -1) {} // Noncompliant {{Remove this comparison; it will always return true.}}

    if (c > 1000) {} // Compliant - within range
    if (c < 60000) {} // Compliant
  }

  void intComparisons(int i) {
    if (i > 2147483648L) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (i >= 2147483648L) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (i < -2147483649L) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (i == 2147483648L) {} // Noncompliant {{Remove this comparison; it will always return false.}}

    if (i < 2147483648L) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (i <= 2147483647L) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (i >= -2147483648L) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (i != 2147483648L) {} // Noncompliant {{Remove this comparison; it will always return true.}}

    if (i > 1000000) {} // Compliant
    if (i < -1000000) {} // Compliant
  }

  void reversedOperands(byte b) {
    if (200 < b) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (200 <= b) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (-200 > b) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (200 > b) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (200 >= b) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (200 == b) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (200 != b) {} // Noncompliant {{Remove this comparison; it will always return true.}}

    if (100 == b) {} // Compliant - 100 within byte range
    if (100 != b) {} // Compliant
    if (100 < b) {} // Compliant
    if (-100 > b) {} // Compliant
  }

  void longVariable(long l) {
    if (l > 9223372036854775807L) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (l >= Long.MIN_VALUE) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (l < Long.MIN_VALUE) {} // Noncompliant {{Remove this comparison; it will always return false.}}

    if (l > 2147483648L) {} // Compliant - long can hold this value
    if (l < -2147483649L) {} // Compliant
    if (l != 9223372036854775807L) {} // Compliant - Long.MAX_VALUE is reachable
  }

  void integralVariableAgainstFloatingConstant(byte b, int i, long l) {
    if (b > 200.5) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (b <= -128.5) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (b < 127.5) {} // Noncompliant {{Remove this comparison; it will always return true.}}
    if (i > 1e10) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (l > 1e30) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (l < 1e30) {} // Noncompliant {{Remove this comparison; it will always return true.}}

    if (b > Float.MAX_VALUE) {} // Noncompliant {{Remove this comparison; it will always return false.}}

    if (b > 126.5) {} // Compliant - b can be 127
    if (b == 100.0) {} // Compliant
    if (i > 1e5) {} // Compliant
    if (l > 1e18) {} // Compliant - long can hold this value
    if (b > Double.NaN) {} // Compliant - not modelled, although every comparison with NaN is false
  }

  void boxedVariable(Integer boxed, Byte boxedByte) {
    if (boxed == 5) {} // Compliant - not a primitive type
    if (boxedByte > 200) {} // Compliant - not a primitive type
  }

  // A float or double variable can hold +/-Infinity, so nothing can be concluded from the finite range of the
  // type: "f > 1e40" is true when f is +Infinity. Equality would be decidable, but every equality test on a
  // float or double is already reported by S1244, so floating-point variables are out of scope entirely.
  void floatVariable(float f) {
    if (f > 1e40) {} // Compliant - true when f is +Infinity
    if (f < 1e40) {} // Compliant - false when f is +Infinity
    if (f > Float.MAX_VALUE) {} // Compliant - a legitimate way to test for +Infinity
    if (f == 1e40) {} // Compliant - covered by S1244
    if (f != 1e40) {} // Compliant - covered by S1244
    if (f == Double.MAX_VALUE) {} // Compliant - covered by S1244
    if (f > 1000000) {} // Compliant - within range
  }

  void doubleVariable(double d) {
    if (d > 1000000) {} // Compliant
    if (d > Double.MAX_VALUE) {} // Compliant - a legitimate way to test for +Infinity
    if (d == Double.POSITIVE_INFINITY) {} // Compliant - covered by S1244
  }

  void reversedFloatingOperands(byte b, float f) {
    if (200.5 < b) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (1e40 > f) {} // Compliant - false when f is +Infinity
    if (1e40 == f) {} // Compliant - covered by S1244
  }

  void charAgainstNegativeZero(char c) {
    if (c < -0.0) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (c == -0.0) {} // Compliant - c can be 0, and 0.0 == -0.0
  }

  void intVariableWithinRange(int b) {
    if (b > 200) {} // Compliant - int can hold 200
  }

  void parenthesizedExpressions(byte b) {
    if ((b) > 200) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (b > (200)) {} // Noncompliant {{Remove this comparison; it will always return false.}}
  }

  void constantExpressions(byte b) {
    if (b > CONSTANT_200) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (b > 127 + 1) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (b > 100 * 3) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (b < -100 - 100) {} // Noncompliant {{Remove this comparison; it will always return false.}}
    if (CONSTANT_200 - 100 > b) {} // Compliant - 100 within byte range
    if (b > 100 + 1) {} // Compliant - 101 within byte range
  }

  void twoVariables(byte a, byte b) {
    if (a > b) {} // Compliant - comparing two variables
  }

  void nonConstantExpression(byte b, int i) {
    if (b > i + 1) {} // Compliant - right operand is not a compile-time constant
  }
}
