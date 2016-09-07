class A {

  boolean foo(boolean a) {
    if(a) {
      return a;
    } else {
      return a;
    }
  }

  void qix() {
    boolean b = foo(true);
    if (b) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    boolean c = foo(false);
    if (c) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
  }

}