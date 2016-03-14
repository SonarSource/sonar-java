class A {
  void foo() {
    boolean a = true;
    System.exit(-1);
    if (a) {

    }
  }

  void bar(boolean a) {
    if (a) {
      System.exit(-1);
    }
    if(a) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
  }
}
