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
  }

  abstract void qix(boolean a);
}
