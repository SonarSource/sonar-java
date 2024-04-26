package checks;

public class MathClampMethodsCheckSample {

  public void testNoncompliant(int min, int max, int value) {
    int clampedValueConditional1 = value > max ? max : value < min ? min : value; // Noncompliant {{Use "Math.clamp" instead of a conditional expression.}}
//                                 ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    int clampedValueConditional2 = value < min ? min : value > max ? max : value; // Noncompliant

    int clampedValueConditional4 = value > max ? max : Math.min(min, value); // Compliant
    int clampedValueConditional5 = value < min ? Math.max(max, value) : value; // Compliant
    int clampedValueConditional6 = value < min ? Math.max(max, value) : value > max ? max : value; // Compliant
    int clampedValueConditional7 = value < min ? Math.max(max, value) : value < max ? value : max; // Compliant

    int conditionalValue = value == max ? max : value; // Compliant
    int conditionalValue2 = value > max ? max : value; // Compliant

    int clampedValueMethodInvocation1 = Math.min(min, Math.max(max, value)); // Noncompliant {{Use "Math.clamp" instead of "Math.min" or "Math.max".}}
//                                      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    int clampedValueMethodInvocation2 = Math.max(min, Math.min(max, value)); // Noncompliant

    int clampedValueMethodInvocation3 = Math.min(Math.min(min, value), max); // Compliant - FN
    int clampedValueMethodInvocation4 = Math.max(Math.max(min, value), min); // Compliant - FN

    int clampValueMethodInvocation5 = Math.min(Math.max(min, value), min > value ? min : value); // Noncompliant
    int clampValueMethodInvocation6 = Math.max(Math.min(min, value), max < value ? max : value); // Noncompliant
    int clampValueMethodInvocation7 = Math.min(max < value ? max : value, Math.min(min, value)); // Compliant
    int a = Math.max(min, Math.max(value, max)); // Compliant
    int maxValue = Math.max(Math.max(value, min), max); // Compliant

    int nonClamped1 = Math.min(min, value); // Compliant
    int nonClamped2 = Math.max(value, max); // Compliant
    double rand = Math.random(); // Compliant
    int methodInvocation = getMin(); // Compliant
    double clamped = Double.min(min, Double.max(max, value));// Compliant

    int clampedMath = Math.clamp(value, min, max); // Compliant
  }

  private void testConditional(int min, int max, int value) {
    int a1 = value > max ? max : value < min ? min : value; // Noncompliant
    int a2 = value > max ? max : value > min ? value : min; // Noncompliant
    int a3 = value > max ? max : Math.max(min, value); // Noncompliant

    int a4 = value > min ? value < max ? value : max : min; // Noncompliant
    int a5 = value > min ? value > max ? max : value : min; // Noncompliant
    int a6 = value > min ? Math.min(value, max) : min; // Noncompliant

    // -------

    int a7 = value < min ? min : value > max ? max : value; // Noncompliant
    int a8 = value < min ? min : value < max ? value : max; // Noncompliant

    int a9 = value < min ? min : Math.min(max, value); // Noncompliant

    int a10 = value < max ? value > min ? value : min : max; // Noncompliant
    int a11 = value < max ? value < min ? min : value : max; // Noncompliant

    int a12 = value < max ? Math.max(min, value) : max; // Noncompliant

    int b1 = value > max ? max : value; // Compliant
    int b2 = value > max ? value : value < min ? min : value; // Compliant
    int b3 = value > max ? max : value > max ? value : max; // Noncompliant

    int maxValue = value > min ? min : value > max ? max : value; // Compliant

    int b4 = value > max ? max : (value < min ? min : value); // Noncompliant

    // -----
    int c1 = max > value ? min > value ? min : value : max; // Noncompliant
    int c2 = max > value ? min < value ? value : min : max; // Noncompliant
    int c3 = max < value ? max : min > value ? min : value; // Noncompliant
    int c4 = max < value ? max : min < value ? value : min; // Noncompliant

    int c11 = max > value ? value > min ? value : min : max; // Noncompliant
    int c12 = max > value ? value < min ? min : value : max; // Noncompliant
    int c13 = max < value ? max : value > min ? value : min; // Noncompliant
    int c14 = max < value ? max : value < min ? min : value; // Noncompliant

    int c5 = min > value ? min : max > value ? value : max; // Noncompliant
    int c6 = min > value ? min : max < value ? max : value; // Noncompliant
    int c7 = min < value ? max > value ? value : max : min; // Noncompliant
    int c8 = min < value ? max < value ? max : value : min; // Noncompliant

    int c15 = min > value ? min : value > max ? max : value; // Noncompliant
    int c16 = min > value ? min : value < max ? value : max; // Noncompliant
    int c17 = min < value ? value > max ? max : value : min; // Noncompliant
    int c18 = min < value ? value < max ? value : max : min; // Noncompliant

    var testCases = new int[] {
      value > max ? max : (value < min ? min : value), // Noncompliant
      max < value ? max : (value < min ? min : value), // Noncompliant
      value <= max ? (value < min ? min : value) : max, // Noncompliant
      max >= value ? (value < min ? min : value) : max, // Noncompliant
      max >= value ? (min > value ? min : value) : max, // Noncompliant
      max >= value ? (value >= min ? value : min) : max, // Noncompliant
      value > max ? max : (min > value ? min : value), // Noncompliant
      value > max ? max : (value >= min ? value : min), // Noncompliant
      value > max ? max : (min <= value ? value : min) // Noncompliant
    };

    int t1 = value > min ? value > max ? value : max : min; // Complaint - coverage
    int t2 = value > min ? value > max ? value : value : min; // Complaint - coverage
    int t3 = value > min ? value > max ? max : max : min; // Complaint - coverage

    int t4 = value > max ? max : value > min ? min : value; // Complaint - coverage
    int t5 = value > max ? max : value > min ? value : value; // Complaint - coverage
    int t6 = value > max ? max : value > min ? min : min; // Complaint - coverage
  }

  private int getMin() {
    return 0;
  }

}
