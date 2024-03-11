package checks;

public class MathClampMethodsCheckSample {

  public void testNoncompliant(int min, int max, int value) {
    int clampedValueConditional1 = value > max ? max : value < min ? min : value; // Noncompliant [[sc=36;ec=81]] {{Use "Math.clamp" instead of a conditional expression.}}
    int clampedValueConditional2 = value < min ? min : value > max ? max : value; // Noncompliant

    int clampedValueConditional4 = value > max ? max : Math.min(min, value); // Compliant
    int clampedValueConditional5 = value < min ? Math.max(max, value) : value; // Compliant
    int clampedValueConditional6 = value < min ? Math.max(max, value) : value > max ? max : value; // Compliant
    int clampedValueConditional7 = value < min ? Math.max(max, value) : value < max ? value : max; // Compliant

    int conditionalValue = value == max ? max : value; // Compliant
    int conditionalValue2 = value > max ? max : value; // Compliant

    int clampedValueMethodInvocation1 = Math.min(min, Math.max(max, value)); // Noncompliant [[sc=41;ec=76]] {{Use "Math.clamp" instead of "Math.min" or "Math.max".}}
    int clampedValueMethodInvocation2 = Math.max(min, Math.min(max, value)); // Noncompliant

    int clampedValueMethodInvocation3 = Math.min(Math.min(min, value), max); // Compliant - FN
    int clampedValueMethodInvocation4 = Math.max(Math.max(min, value), min); // Compliant - FN

    int clampValueMethodInvocation5 = Math.min(Math.max(min, value), min > value ? min : value); // Noncompliant
    int clampValueMethodInvocation6 = Math.max(Math.min(min, value), max < value ? max : value); // Noncompliant
    int clampValueMethodInvocation7 = Math.min(max < value ? max : value, Math.min(min, value)); // Compliant
    int a = Math.max(min, Math.max(value, max)); // Compliant
    int maxValue = Math.max(Math.max(value,min),max); // Compliant

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
    int b3 = value > max ? max : value > max ? value : max; // Noncompliant - stupid FP

    int maxValue = value > min ? min : value > max ? max : value; // Compliant

    int b4 = value > max ? max : (value < min ? min : value); // Noncompliant
  }

  private int getMin() {
    return 0;
  }

}
