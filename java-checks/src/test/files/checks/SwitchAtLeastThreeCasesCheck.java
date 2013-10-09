class A {
  public void f() {
    switch (variable) { // Noncompliant
      case 0:
        doSomething();
        break;
      default:
        doSomethingElse();
        break;
    }

    switch (variable) { // Compliant
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

    if (variable == 0) { // Compliant
      doSomething();
    } else {
      doSomethingElse();
    }
  }
}
