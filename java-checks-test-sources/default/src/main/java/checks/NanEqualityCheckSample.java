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

}
