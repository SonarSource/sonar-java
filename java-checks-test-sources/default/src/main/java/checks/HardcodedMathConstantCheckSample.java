package checks;

class HardcodedMathConstantCheckSample {

  // Pi approximations at various precisions
  double pi1 = 3.14; // Noncompliant {{Use "Math.PI" instead of this approximation of pi.}}
  double pi2 = 3.14159; // Noncompliant {{Use "Math.PI" instead of this approximation of pi.}}
  double pi3 = 3.14159265; // Noncompliant {{Use "Math.PI" instead of this approximation of pi.}}
  double pi4 = 3.14159265358979; // Noncompliant {{Use "Math.PI" instead of this approximation of pi.}}
  double pi5 = 3.141; // Noncompliant {{Use "Math.PI" instead of this approximation of pi.}}

  // E approximations
  double e1 = 2.718; // Noncompliant {{Use "Math.E" instead of this approximation of Euler's number.}}
  double e2 = 2.71828; // Noncompliant {{Use "Math.E" instead of this approximation of Euler's number.}}
  double e3 = 2.71828182845; // Noncompliant {{Use "Math.E" instead of this approximation of Euler's number.}}

  // sqrt(2) approximations
  double sqrt2_1 = 1.414; // Noncompliant {{Use "Math.sqrt(2)" instead of this approximation of the square root of 2.}}
  double sqrt2_2 = 1.41421; // Noncompliant {{Use "Math.sqrt(2)" instead of this approximation of the square root of 2.}}
  double sqrt2_3 = 1.4142135; // Noncompliant {{Use "Math.sqrt(2)" instead of this approximation of the square root of 2.}}

  // ln(2) approximations
  double ln2_1 = 0.693; // Noncompliant {{Use "Math.log(2)" instead of this approximation of the natural logarithm of 2.}}
  double ln2_2 = 0.69314; // Noncompliant {{Use "Math.log(2)" instead of this approximation of the natural logarithm of 2.}}
  double ln2_3 = 0.693147; // Noncompliant {{Use "Math.log(2)" instead of this approximation of the natural logarithm of 2.}}

  // Float literals
  float piFloat = 3.14159f; // Noncompliant {{Use "Math.PI" instead of this approximation of pi.}}
  float eFloat = 2.718f; // Noncompliant {{Use "Math.E" instead of this approximation of Euler's number.}}

  // Static final field
  private static final double MY_PI = 3.14159265358979; // Noncompliant

  // In arithmetic expressions
  double area(double r) {
    return 3.14159 * r * r; // Noncompliant
  }

  double circumference(double r) {
    return 2 * 3.14159 * r; // Noncompliant
  }

  double volume(double r) {
    return (4.0 / 3.0) * 3.14159 * r * r * r; // Noncompliant
  }

  // Method arguments
  double shifted(double x) {
    return Math.sin(x + 3.14159); // Noncompliant
  }

  // Ternary expression
  double pick(boolean b, double x) {
    return b ? 3.14159 : x; // Noncompliant
  }

  // Compliant - standard library constants
  double compliantPi = Math.PI;
  double compliantE = Math.E;
  double compliantSqrt2 = Math.sqrt(2);
  double compliantLn2 = Math.log(2);
  double compliantStrictPi = StrictMath.PI;
  double compliantStrictE = StrictMath.E;

  // Compliant - unrelated values
  double unrelated1 = 3.0;
  double unrelated2 = 2.0;
  double unrelated3 = 1.5;
  double unrelated4 = 0.5;
  double unrelated5 = 100.0;
  double unrelated6 = 0.001;

  // Compliant - too few significant digits
  double tooImprecise1 = 3.1;
  double tooImprecise2 = 2.7;
  double tooImprecise3 = 1.4;
  double tooImprecise4 = 0.69;

  // Compliant - outside tolerance
  double outsideTolerance1 = 3.15;
  double outsideTolerance2 = 1.41;

  // Compliant - scientific notation (skipped)
  double sci1 = 3.14e0;
  double sci2 = 314e-2;

  // Compliant - hex float literals (skipped)
  double hex1 = 0x1.0p0;
}
