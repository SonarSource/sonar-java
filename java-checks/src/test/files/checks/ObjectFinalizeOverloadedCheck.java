class A {
  public void finalize() { // Compliant
  }

  @Override
  public void finalize() { // Compliant
  }

  public void finalize(int a) { // Noncompliant [[sc=15;ec=23]] {{Rename this method to avoid any possible confusion with Object.finalize().}}
  }

  private void finalize(int a, int b) { // Noncompliant {{Rename this method to avoid any possible confusion with Object.finalize().}}
  }

  private int finalize() { // Noncompliant
    return 0;
  }

  private int finalize(int a) { // Noncompliant {{Rename this method to avoid any possible confusion with Object.finalize().}}
    return a;
  }

  private void foo() { // Compliant
  }

  private int foo(int a) { // Compliant
    return a;
  }
}

class B {
  private Object finalize() { // Noncompliant
    return null;
  }
}
