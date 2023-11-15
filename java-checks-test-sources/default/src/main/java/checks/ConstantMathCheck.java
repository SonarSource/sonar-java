package checks;

public class ConstantMathCheck {

  public void method(int a, Integer integer, Double doubleParam) {
    byte b = (byte) (a % 1); // Noncompliant [[sc=24;ec=25]] {{Remove this computation of % 1, which always evaluates to zero.}}
    double v = 7.4d;
    double remainder = v % 1; // compliant, remainder is ~0.4
    b = (byte) (integer % 1); // Noncompliant
    b = (byte) (doubleParam % 1); // Compliant, double
    int c = a % 2; // Compliant
    int d = a % a; // Compliant, currently not covered by this rule
    short s = 0x7fff;

    Math.abs((double)' '); // Noncompliant [[sc=5;ec=13]] {{Remove this unnecessary call to "Math.abs"}}
    Math.abs((float) 0); // Noncompliant {{Remove this unnecessary call to "Math.abs"}}
    Math.abs(0); // Noncompliant {{Remove this unnecessary call to "Math.abs"}}
    Math.abs(0L); // Noncompliant {{Remove this unnecessary call to "Math.abs"}}
    Math.abs(a); // Compliant
    Math.abs((float) a); // Compliant
    Math.ceil((double) new Object()); // Compliant

    Math.ceil(a); // Noncompliant {{Remove this unnecessary call to "Math.ceil"}}
    Math.ceil((int) a); // Noncompliant {{Remove this unnecessary call to "Math.ceil"}}
    Math.ceil((double) b); // Noncompliant {{Remove this unnecessary call to "Math.ceil"}}
    Math.ceil((double) ' '); // Noncompliant {{Remove this unnecessary call to "Math.ceil"}}
    Math.ceil((double) 0); // Noncompliant {{Remove this unnecessary call to "Math.ceil"}}
    Math.ceil((double) 0L); // Noncompliant {{Remove this unnecessary call to "Math.ceil"}}
    Math.ceil((double) s); // Noncompliant {{Remove this unnecessary call to "Math.ceil"}}
    Math.ceil((double) 0.0f); // Compliant
    Math.ceil((double) 0.0d); // Compliant
    Math.ceil((float) (byte) 0); // Noncompliant {{Remove this unnecessary call to "Math.ceil"}}
    Math.floor((float) 0); // Noncompliant {{Remove this unnecessary call to "Math.floor"}}
    Math.rint((float) 0); // Noncompliant {{Remove this unnecessary call to "Math.rint"}}
    Math.round((float) 0); // Noncompliant {{Remove this unnecessary call to "Math.round"}}
    Math.ceil(((double) ((double) (a)))); // Noncompliant {{Remove this unnecessary call to "Math.ceil"}}

    float value = 3.14f;
    Math.acos(value); // Compliant
    Math.cos(value); // Compliant
    Math.acos(2.0); // Compliant
    Math.cos(1.0); // Compliant
    Math.cos(2.0); // Compliant

    Math.acos((0.0)); // Noncompliant {{Remove this unnecessary call to "Math.acos"}}
    Math.acos((0.0F)); // Noncompliant {{Remove this unnecessary call to "Math.acos"}}
    Math.acos((1.0)); // Noncompliant {{Remove this unnecessary call to "Math.acos"}}
    Math.acos((1.0f)); // Noncompliant {{Remove this unnecessary call to "Math.acos"}}
    Math.asin(0.0d); // Noncompliant {{Remove this unnecessary call to "Math.asin"}}
    Math.asin(1.0d); // Noncompliant {{Remove this unnecessary call to "Math.asin"}}
    Math.atan(0.0d); // Noncompliant {{Remove this unnecessary call to "Math.atan"}}
    Math.atan(1.0d); // Noncompliant {{Remove this unnecessary call to "Math.atan"}}
    Math.atan2(0.0D, value); // Noncompliant {{Remove this unnecessary call to "Math.atan2"}}
    Math.cbrt(0.0d); // Noncompliant {{Remove this unnecessary call to "Math.cbrt"}}
    Math.cbrt(1.0d); // Noncompliant {{Remove this unnecessary call to "Math.cbrt"}}
    Math.cos((0.0d)); // Noncompliant {{Remove this unnecessary call to "Math.cos"}}
    Math.cosh(0.0d); // Noncompliant {{Remove this unnecessary call to "Math.cosh"}}
    Math.exp(0.0d); // Noncompliant {{Remove this unnecessary call to "Math.exp"}}
    Math.exp(1.0d); // Noncompliant {{Remove this unnecessary call to "Math.exp"}}
    Math.expm1(0.0d); // Noncompliant {{Remove this unnecessary call to "Math.expm1"}}
    Math.log(0.0d); // Noncompliant {{Remove this unnecessary call to "Math.log"}}
    Math.log(1.0d); // Noncompliant {{Remove this unnecessary call to "Math.log"}}
    Math.log10(0.0d); // Noncompliant {{Remove this unnecessary call to "Math.log10"}}
    Math.log10(1.0d); // Noncompliant {{Remove this unnecessary call to "Math.log10"}}
    Math.sin(0.0d); // Noncompliant {{Remove this unnecessary call to "Math.sin"}}
    Math.sinh(0.0d); // Noncompliant {{Remove this unnecessary call to "Math.sinh"}}
    Math.sqrt(0.0d); // Noncompliant {{Remove this unnecessary call to "Math.sqrt"}}
    Math.sqrt(1.0d); // Noncompliant {{Remove this unnecessary call to "Math.sqrt"}}
    Math.tan(0.0d); // Noncompliant {{Remove this unnecessary call to "Math.tan"}}
    Math.tanh(0.0d); // Noncompliant {{Remove this unnecessary call to "Math.tanh"}}
    Math.toDegrees(0.0d); // Noncompliant {{Remove this unnecessary call to "Math.toDegrees"}}
    Math.toDegrees(1.0d); // Noncompliant {{Remove this unnecessary call to "Math.toDegrees"}}
    Math.toRadians(0.0d); // Noncompliant {{Remove this unnecessary call to "Math.toRadians"}}
  }
}
