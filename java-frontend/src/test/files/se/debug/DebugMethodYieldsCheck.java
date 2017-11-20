abstract class A {
  static Object foo(boolean b) { // Noncompliant {{Method 'foo' has 2 method yields.}}
    if (b) {
      return null;
    }
    return new Object();
  }

  Object bar(boolean b) { // Compliant - we do not have method behaviors for overrideable methods
    if (a) {
      return null;
    }
    return new Object();
  }

  abstract Object qix(boolean b); // Compliant
}
