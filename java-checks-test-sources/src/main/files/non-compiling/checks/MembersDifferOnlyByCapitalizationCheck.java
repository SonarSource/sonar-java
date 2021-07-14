class MembersDifferOnlyByCapitalizationCheck {
  Object m;
  Object f1, f2, f3;
  Object flambda;
  Object fAnon;

  Object m() {  // Compliant
    return m;
  }

  Object M() { // Noncompliant
    return m;
  }

  Object f1() { // Noncompliant
    return new Object();
  }

  Object f2() { // Noncompliant
    return;  // just for coverage, doesn't compile
  }

  Object flambda() { // Compliant
    call(f -> {
      return new Object();
    });
    return flambda;
  }

  Object fAnon() { // Compliant
    Object o = new Object() {
      String toString() {
        return "";
      }
    };
    return fAnon;
  }

  Object foo;
  void foo() { // Noncompliant
    SameName sn = new SameName() {
      Object bar() {
        return foo;
      }
    };
  }

  Object f3() { // Noncompliant
    if (cond) {
      return new Object();
    }
    return f3;
  }
}
