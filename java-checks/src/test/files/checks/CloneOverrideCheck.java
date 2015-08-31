class A {
  public Object clone(){  // Noncompliant
    return super.clone();
  }
  public Object clonerMethod() {  // Compliant
  }
  public Object clone(int a) {  // Compliant
  }
}
