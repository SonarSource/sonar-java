class A {

  private boolean foo(boolean a) {
    if (a) {
      return a;
    } else {
      return a;
    }
  }

  public boolean bar() {
    return true;
  }

  public final boolean gul() {
    return true;
  }

  void qix() {
    boolean b = foo(true);
    if (b) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    boolean c = foo(false);
    if (c) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }

    boolean d = bar();
    if (d) { // Compliant - method bar is public allowed for extension
    }

    boolean e = gul();
    if (e) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
  }

}

final class B {
  public boolean bar() {
    return true;
  }

  private void qix() {
    boolean b = bar();
    if (b) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
  }
}
