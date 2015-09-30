class A {
  public void f() {
    switch (variable) { // Noncompliant {{Replace this "switch" statement by "if" statements to increase readability.}}
      case 0:
        doSomething();
        break;
      default:
        doSomethingElse();
        break;
    }

    switch (variable) {
      case 0:
      case 1:
        doSomething();
        break;
      default:
        doSomethingElse();
        break;
    }

    switch (variable) { // Noncompliant
    }

    if (variable == 0) {
      doSomething();
    } else {
      doSomethingElse();
    }
  }
}
