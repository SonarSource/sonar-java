abstract class A {
  public abstract int alpha(long l); // Compliant - not native

  native int beta(int i); // Compliant - not public
  native public int gamma(boolean b); // Noncompliant {{Make this native method private and provide a wrapper.}}
  native private int delta(String s, int i, boolean b);

  int bar(int i) { // Noncompliant [[sc=7;ec=10;secondary=4]] {{Make this wrapper for native method 'beta' less trivial.}}
    return beta(i);
  }

  void qix(int i) { // Noncompliant [[sc=8;ec=11;secondary=4]] {{Make this wrapper for native method 'beta' less trivial.}}
    (beta(i));
  }

  int kel(int i, String s, boolean b) { // Noncompliant [[sc=7;ec=10;secondary=6]] {{Make this wrapper for native method 'delta' less trivial.}}
    return delta(s, i, b);
  }

  void foo(String s) { // Compliant - unknown method
    unknownMethod(s);
  }

  int tol(long l) { // Compliant - not a wrapper
    return alpha(l);
  }

  int mon(int i) { // Compliant - not a wrapper
    return i;
  }

  void nul(int i) { // Compliant - not a wrapper
  }

  int xor(int i) { // Compliant - not a wrapper
    return;
  }

  int gul(String s) { // Compliant - wrapper is 'not' trivial
    return delta(s, s.length(), s.isEmpty());
  }

  boolean field = false;
  int pam(String s, int i) { // Compliant - wrapper is 'not' trivial
    return delta(s, i, field);
  }

  int lol(int i) { // Compliant - wrapper is 'not' trivial
    doSomething(i);
    return beta(i);
  }

  int zut(int i) { // Compliant - wrapper is 'not' trivial
    if (i > 42) {
      return beta(i);
    } else {
      return beta(i + 42);
    }
  }

  abstract void doSomething(Object ... objects);
}
