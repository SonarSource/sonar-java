class A {

  public void foo() {
    foo.equals(null); // Noncompliant
    foo.equals(0); // Compliant
    equals(null); // Compliant
    foo.equals(null, 0); // Compliant
  }

}
