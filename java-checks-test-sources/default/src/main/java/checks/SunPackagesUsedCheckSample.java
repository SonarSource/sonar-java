package checks;

class SunPackagesUsedCheckSample {
  private Object sun; // variable named "sun"

  // SONARJAVA-4698: Variables named "sun" should not trigger the rule
  public void fooWithFieldNamedSun() {
    sun.toString(); // Compliant - "sun" is a field of type Object, not a sun.* package class
  }

  public void barWithParameterNamedSun(Object sun) {
    sun.toString(); // Compliant - "sun" is a parameter of type Object, not a sun.* package class
  }

  public void bazWithLocalVariableNamedSun() {
    Object sun = new Object();
    sun.toString(); // Compliant - "sun" is a local variable of type Object, not a sun.* package class
  }

  // Actual sun.* package usage - should trigger the rule
  public void useSunMiscUnsafe() {
    sun.misc.Unsafe unsafe = null; // Noncompliant {{Use classes from the Java API instead of Sun classes.}}
//  ^^^^^^^^^^^^^^^
  }
}
