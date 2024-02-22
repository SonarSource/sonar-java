package checks;

public class MathClampMethodsCheckSample {

  public void testNoncompliant(int min, int max, int value) {
    int clampedValueConditional1 = value > max ? max : value < min ? min : value; // Noncompliant [[sc=36;ec=81]] {{Use "Math.clamp" instead of a conditional expression.}}
    int clampedValueConditional2 = value < min ? min : value > max ? max : value; // Noncompliant
    int clampedValueConditional3 = value < min ? value < min ? min : value : max; // Noncompliant
    int conditionalValue = value == max ? max : value; // Compliant

    int clampedValueMethodInvocation1 = Math.min(min, Math.max(max, value)); // Noncompliant [[sc=41;ec=76]] {{Use "Math.clamp" instead of "Math.min" or "Math.max".}}
    int clampedValueMethodInvocation2 = Math.max(min, Math.min(max, value)); // Noncompliant
    int clampedValueMethodInvocation3 = Math.min(Math.min(min, value), max); // Noncompliant
    int clampedValueMethodInvocation4 = Math.max(Math.max(min, value), min); // Noncompliant

    int nonClamped1 = Math.min(min, value); // Compliant
    int nonClamped2 = Math.max(value, max); // Compliant
    double rand = Math.random(); // Compliant
    int methodInvocation = getMin(); // Compliant
    double clamped = Double.min(min, Double.max(max, value));// Compliant

    int clampedValue = value;
    if (value > max) { // Noncompliant [[sc=5;ec=6]] {{Use "Math.clamp" instead of a conditional expression.}}
      clampedValue = max;
    } else if (value < min) {
      clampedValue = min;
    }

    if (value == max) { // Compliant
      clampedValue = max;
    }

    if (value < max) {
      clampedValue = value;
    } else {
      clampedValue = min;
    }

    if (value > min) {
      clampedValue = value;
    } else if (value == max) {

    }

    int clampedMath = Math.clamp(value, min, max); // Compliant
  }

  private int getMin() {
    return 0;
  }

}
