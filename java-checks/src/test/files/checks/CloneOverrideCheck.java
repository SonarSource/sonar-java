class A {
  public Object clone(){  // Noncompliant [[sc=17;ec=22]] {{Remove this "clone" implementation; use a copy constructor or copy factory instead.}}
    return super.clone();
  }
  public Object clonerMethod() {  // Compliant
  }
  public Object clone(int a) {  // Compliant
  }
}
