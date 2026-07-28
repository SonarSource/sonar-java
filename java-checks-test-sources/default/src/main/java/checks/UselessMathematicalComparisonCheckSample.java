package checks;

class UselessMathematicalComparisonCheckSample {

  static final int CONSTANT_200 = 200;

  void byteComparisons(byte b) {
    if (b > 200) {} // Noncompliant {{Remove this comparison; it will always return false.}}
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
  }

  void longVariable(long l) {
    if (l > 2147483648L) {} // Compliant - long can hold this value
    if (l < -2147483649L) {} // Compliant
  }

  void floatVariable(float f) {
    if (f > 1000000) {} // Compliant - float/double not checked
  }

  void doubleVariable(double d) {
    if (d > 1000000) {} // Compliant - float/double not checked
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
  }

  void twoVariables(byte a, byte b) {
    if (a > b) {} // Compliant - comparing two variables
  }
}
