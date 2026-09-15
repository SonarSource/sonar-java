package checks;

class S9395CheckSample {

  void variableInitialization() {
    int intVar = 100_000_000;
    long longVar = 100_000_000_000L;

    float f1 = intVar; // Noncompliant {{Explicitly cast this "int" to "float" to document potential precision loss.}}
//             ^^^^^^
    float f2 = longVar; // Noncompliant {{Explicitly cast this "long" to "float" to document potential precision loss.}}
//             ^^^^^^^
    double d1 = longVar; // Noncompliant {{Explicitly cast this "long" to "double" to document potential precision loss.}}
//              ^^^^^^^

    double d2 = intVar; // compliant: int -> double is not lossy
    float f3 = (float) intVar; // compliant: explicit cast
    float f4 = (float) longVar; // compliant: explicit cast
    double d3 = (double) longVar; // compliant: explicit cast

    byte byteVar = 1;
    short shortVar = 1;
    char charVar = 'a';
    float f5 = byteVar; // compliant: byte fits in float
    float f6 = shortVar; // compliant: short fits in float
    float f7 = charVar; // compliant: char fits in float
    double d4 = byteVar; // compliant: byte fits in double
    double d5 = shortVar; // compliant: short fits in double

    float f8 = 42; // compliant: small literal within range
    float f9 = 16_777_216; // compliant: exactly 2^24, representable
    float f10 = 16_777_217; // Noncompliant
//              ^^^^^^^^^^
    float f11 = 3.14f; // compliant: already float

    double d6 = 42L; // compliant: small long literal within range
    double d7 = 9_007_199_254_740_992L; // compliant: exactly 2^53, representable
    double d8 = 9_007_199_254_740_993L; // Noncompliant
//              ^^^^^^^^^^^^^^^^^^^^^^

    float f12 = 0; // compliant: zero

    long longLit = 100L;
    float f13 = longLit; // Noncompliant
    float f14 = 100L; // compliant: small long literal

    int intVal = 42;
    double d9 = intVal; // compliant: int -> double is safe
  }

  void assignment() {
    int intVar = 100_000_000;
    long longVar = 100_000_000_000L;

    float floatField;
    double doubleField;

    floatField = intVar; // Noncompliant
//               ^^^^^^
    floatField = longVar; // Noncompliant
    doubleField = longVar; // Noncompliant
    doubleField = intVar; // compliant
    floatField = (float) intVar; // compliant
  }

  void compoundAssignment() {
    long longVar = 100_000_000_000L;
    int intVar = 100;
    float floatVar = 1.0f;
    double doubleVar = 1.0;

    floatVar += intVar; // Noncompliant
//              ^^^^^^
    floatVar += longVar; // Noncompliant
    doubleVar += longVar; // Noncompliant
    doubleVar += intVar; // compliant
    floatVar += (float) intVar; // compliant

    floatVar -= longVar; // Noncompliant
    floatVar *= intVar; // Noncompliant
    doubleVar -= longVar; // Noncompliant
  }

  void methodArguments(int intVar, long longVar) {
    takeFloat(intVar); // Noncompliant
//            ^^^^^^
    takeFloat(longVar); // Noncompliant
    takeDouble(longVar); // Noncompliant
    takeDouble(intVar); // compliant
    takeFloat((float) intVar); // compliant
    takeFloat(42); // compliant: small literal
  }

  void takeFloat(float f) {}
  void takeDouble(double d) {}

  void constructorArguments(int intVar, long longVar) {
    new FloatHolder(intVar); // Noncompliant
//                  ^^^^^^
    new FloatHolder(longVar); // Noncompliant
    new DoubleHolder(longVar); // Noncompliant
    new DoubleHolder(intVar); // compliant
    new FloatHolder((float) intVar); // compliant
  }

  static class FloatHolder {
    FloatHolder(float f) {}
  }

  static class DoubleHolder {
    DoubleHolder(double d) {}
  }

  float returnIntAsFloat(int intVar) {
    return intVar; // Noncompliant
//         ^^^^^^
  }

  float returnLongAsFloat(long longVar) {
    return longVar; // Noncompliant
  }

  double returnLongAsDouble(long longVar) {
    return longVar; // Noncompliant
  }

  double returnIntAsDouble(int intVar) {
    return intVar; // compliant
  }

  float returnExplicitCast(int intVar) {
    return (float) intVar; // compliant
  }

  float returnSmallLiteral() {
    return 42; // compliant
  }

  void ternaryExpression(boolean condition, int intA, int intB) {
    float f = condition ? intA : intB; // Noncompliant
//            ^^^^^^^^^^^^^^^^^^^^^^^
  }

  void parenthesizedExpression(int intVar) {
    float f = (intVar); // Noncompliant
//             ^^^^^^
    float f2 = ((intVar)); // Noncompliant
//               ^^^^^^
  }

  void multipleParams(int intVar, long longVar) {
    takeTwoFloats(intVar, longVar); // Noncompliant 2
//                ^^^^^^
  }

  void takeTwoFloats(float a, float b) {}

  void lambdaReturnDoesNotAffect(int intVar) {
    Runnable r = () -> {
      float f = intVar; // Noncompliant
    };
  }

  void intArithmeticToFloat(int a, int b) {
    float f = a + b; // Noncompliant
//            ^^^^^
  }

  void hexAndBinaryLiterals() {
    float f1 = 0xFF; // compliant: 255 is within range
    float f2 = 0x1000001; // Noncompliant
//             ^^^^^^^^^
    float f3 = 0b1; // compliant: 1 is within range
    int hexMask = 0xFF000000;
    float f4 = hexMask; // Noncompliant
//             ^^^^^^^
    float f5 = 0xFF000000; // compliant: hex literal value -16777216 is representable in float
  }

  void negativeLiterals() {
    float f1 = -1; // compliant: small negative literal
    float f2 = -16_777_216; // compliant: exactly -2^24, representable
    float f3 = -16_777_217; // Noncompliant
//             ^^^^^^^^^^^
    double d1 = -42L; // compliant: small negative long literal
  }

  static final int SMALL_CONSTANT = 100;
  static final int LARGE_CONSTANT = 20_000_000;

  void namedConstants() {
    float f1 = SMALL_CONSTANT; // compliant: named compile-time constant within range
    float f2 = LARGE_CONSTANT; // Noncompliant
//             ^^^^^^^^^^^^^^
  }

  void varargs(int intVar) {
    takeFloatVarargs(intVar, "a", "b"); // Noncompliant
//                   ^^^^^^
    takeFloatVarargs(42, "a"); // compliant: small literal
    takeFloatVarargs((float) intVar, "a"); // compliant: explicit cast
  }

  void takeFloatVarargs(float f, Object... args) {}
}
