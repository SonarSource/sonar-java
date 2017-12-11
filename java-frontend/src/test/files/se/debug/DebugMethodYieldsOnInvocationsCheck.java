import org.apache.commons.lang.StringUtils;

abstract class A {
  static boolean foo(boolean a) {
    if (a) {
      return false;
    }
    return true;
  }

  void bar(boolean a, String s) {
    foo(a); // Noncompliant {{Method 'foo' has 2 method yields.}}
    A.foo(!a); // Noncompliant [[sc=7;ec=10]] {{Method 'foo' has 2 method yields.}}
    StringUtils.isBlank(s); // Noncompliant {{Method 'isBlank' has 2 method yields.}}

    bar(a, s); // Compliant - no yields
    qix(a);
    unknownMethod(a); // Compliant

    // condition always false on next line
    if (gul()) { // Noncompliant {{Method 'gul' has 1 method yields.}}
      // do something
    }

    getNull() // Noncompliant {{Method 'getNull' has 1 method yields.}}
    // NPE on next line
      .gul(); // Noncompliant {{Method 'gul' has 1 method yields.}}

    gul(); // Compliant - NPE triggered 2 lines before. This can not be reached by the engine
  }

  abstract void qix(boolean a);

  static boolean gul() {
    return false;
  }

  static A getNull() {
    return null;
  }
}
