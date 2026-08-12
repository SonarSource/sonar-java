package checks;

import static java.lang.Double.NaN;

class NanEqualityCheckSample {

  void noncompliantDoubleNaN() {
    double result = Math.sqrt(-1);
    if (result == Double.NaN) { } // Noncompliant {{Use "Double.isNaN()" instead of comparison with "Double.NaN".}}
//             ^^
    if (result != Double.NaN) { } // Noncompliant {{Use "Double.isNaN()" instead of comparison with "Double.NaN".}}
//             ^^
  }

  void noncompliantFloatNaN() {
    float value = 0.0f / 0.0f;
    if (value == Float.NaN) { } // Noncompliant {{Use "Float.isNaN()" instead of comparison with "Float.NaN".}}
//            ^^
    if (value != Float.NaN) { } // Noncompliant {{Use "Float.isNaN()" instead of comparison with "Float.NaN".}}
//            ^^
  }

  void noncompliantReversedOperandOrder() {
    double result = 0.0;
    if (Double.NaN == result) { } // Noncompliant {{Use "Double.isNaN()" instead of comparison with "Double.NaN".}}
//                 ^^
    if (Double.NaN != result) { } // Noncompliant {{Use "Double.isNaN()" instead of comparison with "Double.NaN".}}
//                 ^^
  }

  void noncompliantNaNComparedToItself() {
    if (Double.NaN == Double.NaN) { } // Noncompliant
//                 ^^
  }

  void noncompliantInExpressions() {
    double x = 1.0;
    boolean isNan = x == Double.NaN; // Noncompliant
//                    ^^
    boolean notNan = x != Double.NaN; // Noncompliant
//                     ^^
    String s = x != Double.NaN ? "valid" : "invalid"; // Noncompliant
//               ^^
  }

  boolean noncompliantInReturn(double x) {
    return x == Double.NaN; // Noncompliant
//           ^^
  }

  void noncompliantWithParentheses(double x) {
    if (x == (Double.NaN)) { } // Noncompliant
//        ^^
    if ((x) == Double.NaN) { } // Noncompliant
//          ^^
  }

  void compliantIsNaN() {
    double result = Math.sqrt(-1);
    if (Double.isNaN(result)) { } // Compliant
    if (!Double.isNaN(result)) { } // Compliant
  }

  void compliantFloatIsNaN() {
    float value = 0.0f / 0.0f;
    if (Float.isNaN(value)) { } // Compliant
  }

  void compliantRegularComparisons() {
    double a = 1.0;
    double b = 2.0;
    if (a == b) { } // Compliant - regular double comparison
    if (a == 0.0) { } // Compliant - comparison with regular constant
    if (a == Double.POSITIVE_INFINITY) { } // Compliant - comparison with other Double constant
    if (a == Double.MAX_VALUE) { } // Compliant
  }

  void compliantInstanceIsNaN() {
    Double d = Double.valueOf(1.0);
    if (d.isNaN()) { } // Compliant - instance method usage
  }

  void noncompliantStaticImportNaN() {
    double x = 1.0;
    if (x == NaN) { } // Noncompliant {{Use "Double.isNaN()" instead of comparison with "Double.NaN".}}
//        ^^
    if (NaN == x) { } // Noncompliant {{Use "Double.isNaN()" instead of comparison with "Double.NaN".}}
//          ^^
    if (x != NaN) { } // Noncompliant {{Use "Double.isNaN()" instead of comparison with "Double.NaN".}}
//        ^^
  }

  void compliantCustomNaN() {
    CustomClass c = new CustomClass();
    if (c.value == CustomClass.NaN) { } // Compliant - NaN is not from Double or Float
  }

  void compliantLocalVariableNaN() {
    double NaN = -1.0;
    double x = 1.0;
    if (x == NaN) { } // Compliant - NaN is a local variable, not Double.NaN or Float.NaN
  }

  void compliantEqualsOnDoubles() {
    Double a = 1.0;
    Double b = Double.NaN;
    if (a.equals(b)) { } // Compliant - using .equals() method
    if (a.equals(Double.NaN)) { } // Compliant
    if (Double.valueOf(1.0).equals(Double.NaN)) { } // Compliant
    if (a.equals(0.0)) { } // Compliant
  }

  void compliantEqualsOnFloats() {
    Float a = 1.0f;
    Float b = Float.NaN;
    if (a.equals(b)) { } // Compliant - using .equals() method
    if (a.equals(Float.NaN)) { } // Compliant
  }

  void compliantComparisonOperatorsWithNaN() {
    double x = 1.0;
    if (x > Double.NaN) { } // Compliant - rule only covers == and !=
    if (x >= Double.NaN) { } // Compliant
    if (x < Double.NaN) { } // Compliant
    if (x <= Double.NaN) { } // Compliant
    if (Double.NaN > x) { } // Compliant
    if (Double.NaN >= x) { } // Compliant
    if (Double.NaN < x) { } // Compliant
    if (Double.NaN <= x) { } // Compliant
  }

  void compliantComparisonOperatorsWithFloatNaN() {
    float x = 1.0f;
    if (x > Float.NaN) { } // Compliant
    if (x >= Float.NaN) { } // Compliant
    if (x < Float.NaN) { } // Compliant
    if (x <= Float.NaN) { } // Compliant
  }

  static class CustomClass {
    static final double NaN = -1.0;
    double value;
  }

}
