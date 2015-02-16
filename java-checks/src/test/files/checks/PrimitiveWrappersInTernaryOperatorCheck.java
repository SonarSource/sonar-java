class A {
  void foo() {
    Integer i = 123456789;
    Float f = 1.0f;
    Number n = true ? i : f; // Noncompliant
    
    Object o1 = new Object();
    A a2 = new A();
    o1 = true ? o1 : a2; // Compliant
    
    o1 = true ? f : a2; // Compliant
    
    n = true ? (Float) i : f; // Compliant
  }
}