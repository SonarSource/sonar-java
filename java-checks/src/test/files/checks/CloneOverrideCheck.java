class A {
  public Object clone(){  // Noncompliant [[sc=17;ec=22]] {{Remove this "clone" implementation; use a copy constructor or copy factory instead.}}
    return super.clone();
  }
  public Object clonerMethod() {  // Compliant
  }
  public Object clone(int a) {  // Compliant
  }
}
class B {
  @Override
  protected Object clone() throws CloneNotSupportedException { // Compliant, common practice of overriding to throw an exception
    throw new CloneNotSupportedException("Clone not supported for a Singleton");
  }
}
class C {
  @Override
  protected Object clone() throws CloneNotSupportedException { // Noncompliant, not a one statement method
    System.out.println("");
    throw new CloneNotSupportedException("Clone not supported for a Singleton");
  }
}
class D {
  protected abstract Object clone() throws CloneNotSupportedException; // Noncompliant (theoritical case, cannot happen in real life).
}
class E {
  @Override
  protected Object clone() throws CloneNotSupportedException { // Noncompliant, incorrect type of exception
    throw new UnsupportedOperationException("Clone not supported for a Singleton");
  }
}
