package checks;

import sun.misc.Unsafe; // Noncompliant [[secondary=8,11,16,18,23,28,33]]

class SunPackagesUsedCheckWithRealSunUsage {

  // Field declaration with sun.* type
  private sun.misc.Unsafe unsafeField; // secondary

  // Method with sun.* return type
  public sun.misc.Unsafe getUnsafe() { // secondary
    return null;
  }

  // Method with sun.* parameter type
  public void doSomethingWithUnsafe(sun.misc.Unsafe unsafe) { // secondary
    // Method call on sun.* class
    sun.misc.Unsafe.getUnsafe(); // secondary
  }

  // Local variable with sun.* type
  public void localVariable() {
    sun.misc.Unsafe local; // secondary
  }

  // Class literal
  public void classLiteral() {
    Class<?> clazz = sun.misc.Unsafe.class; // secondary
  }

  // Fully qualified type in new expression
  public void newExpression() {
    Object obj = new sun.misc.Signal("INT"); // secondary
  }

  // This should be compliant - variable named "sun"
  public void variableNamedSun() {
    Object sun = new Object();
    sun.toString(); // Compliant - "sun" is a variable, not a sun.* package
  }
}
