package javax.inject;

@interface Inject {} 

class A {
   @Inject
   Object injectedfield; // Noncompliant
}

class B {
  @Inject
  Object injectedField; // Compliant - private constructor 
  
  private B() {}
  
}

class C {
  @Inject
  Object injectedfield; // Noncompliant
  
  Object field; // compliant
  
  void foo() {}
  
  public C() {}
}

class D {
  Object field; // compliant
  
  void foo() {}
  
  public D() {}
}
