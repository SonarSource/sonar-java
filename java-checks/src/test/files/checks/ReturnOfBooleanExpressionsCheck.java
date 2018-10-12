class A {
  void foo() {
    if (something) { // Noncompliant [[sc=5;ec=7]] {{Replace this if-then-else statement by a single return statement.}}
      return true;
    } else {
      return false;
    }

    if (something) { // Noncompliant
      return false;
    } else {
      return true;
    }

    if (something) { // Compliant
      return foo;
    } else {
      return false;
    }

    if (something) { // Compliant
      return true;
    } else {
      return foo;
    }

    if (something) // Noncompliant
      return true;
    else
      return false;

    if (something) { // Compliant
      System.out.println();
    } else {
      return true;
    }

    if (something) { // Compliant
      System.out.println();
      return true;
    } else {
      return false;
    }

    if (something) { // Compliant
      return;
    } else {
      return true;
    }

    if (something) { // Compliant
      return true;
    }

    if (something) { // Noncompliant
      return foo(true);
    } else {
      return foo(false);
    }

    if (something) { // Compliant
      int foo;
    } else {
      return false;
    }
    if (something) // Noncompliant
      return true;
    else
      return false;
  }

  boolean bar() {
    if(something) // Noncompliant
      return true;
    return false;

    if(something) { // Noncompliant
      return true;
    }
    return false;

    if(something) // compliant
      return true;
    System.out.println("");
    return false;

    if(something)
      return true;
  }
}
abstract class B {
  void qix1(Object o) {
    if (bar()) { // Noncompliant
      bar(o, "foo", true);
    } else {
      bar(o, "foo", false);
    }
  }

  void qix2(Object o) {
    if (bar()) { // Compliant
      bar(o, "foo", true);
    } else {
      bar(o, "qix", false);
    }
  }

  void qix3() {
    if (bar()) { // Noncompliant {{Replace this if-then-else statement by a single method invocation.}}
      bar(foo(), "foo", true);
    } else {
      bar(foo(), "foo", false);
    }
  }

  void qix4() {
    if (foo() == null) { // Noncompliant
      bar(foo(), "foo", true);
    } else {
      bar(foo(), "foo", false);
    }
  }

  boolean qix5() {
    if (foo() == null) { // Noncompliant {{Replace this if-then-else statement by a single method invocation.}}
      return bar(foo(), "foo", true);
    } else {
      return (bar(foo(), "foo", false));
    }
  }

  void qix6() {
    if (foo() == null) { // Noncompliant
      bar(foo(), "foo", true);
    } else {
      bar(foo(), "foo", true);
    }
  }

  void qix7(Object o) {
    if (bar()) // Noncompliant
      bar(o, "foo", true);
    else
      bar(o, "foo", false);
  }

  boolean qix8(Object o) {
    if (bar()) {  // Compliant
      return bar(o, "foo", true);
    } else {
      bar(o, "foo", false);
    }
    return false;
  }

  void qix9(Object o) {
    if (bar()) // Compliant
      bar(o, "foo", true);
    else
      bar(o, "foo", bar());
  }

  void qix10(Object o) {
    if (bar()) // Compliant
      bar(o, "foo", bar());
    else
      bar(o, "foo", false);
  }

  void qix11(Object o) {
    if (bar()) // Compliant
      bar(o, "foo", bar());
    else
      bar(o, "foo");
  }

  void qix12(Object o) {
    if (bar()) // Compliant
      bar(o, "foo", true);
    else
      foo(o, "foo", false);
  }

  void qix12(Object o) {
    if (bar()) // Compliant
      bar(o, "foo", bar());
    else
      bar(o, "foo", bar(o, "foo"));
  }

  boolean qix13(Object o) {
    if (bar()) // Compliant
      return bar(o, "foo", true);
    else
      return false;
  }

  boolean qix14(Object o) {
    if (bar()) // Compliant
      return true;
    else
      return bar(o, "foo", false);
  }

  boolean qix15(Object o) {
    if (bar()) // Noncompliant
      bar(o, false, "foo", true);
    else
      bar(o, true, "foo", false);
  }

  boolean qix16(Object o) {
    if (bar()) // Compliant - no boolean literal
      bar(o, "foo");
    else
      bar(o, "foo");
  }

  abstract boolean bar();
  abstract boolean bar(Object o, String s);
  abstract boolean bar(Object o, String s, boolean b);
  abstract boolean bar(Object o, boolean b1, String s, boolean b2);
  abstract Object foo();
  abstract boolean foo(Object o, String s, boolean b);
}
