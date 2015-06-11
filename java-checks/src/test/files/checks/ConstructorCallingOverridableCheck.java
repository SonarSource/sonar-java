class A {
  
  A(A otherA, B b) {
    privateMethod();
    finalMethod();
    this.privateMethod();
    this.finalMethod();
    otherA.privateMethod();
    otherA.finalMethod();
    b.nonFinalPublicMethod();
    staticMethod();
  }
  
  private void privateMethod() {}
  public final void finalMethod() {}
  public static void staticMethod() {}
  
}

class B {
  
  B(B otherB) {
    nonFinalPublicMethod(); // Noncompliant {{Remove this call from a constructor to the overridable "nonFinalPublicMethod" method.}}
    this.nonFinalPublicMethod(); // Noncompliant
    otherB.nonFinalPublicMethod();
    unknownMethod().nonFinalPublicMethod();
  }
  
  public void nonFinalPublicMethod() {}
  
}

class SuperClass {
  
  public final void finalMethod() {}
  public void nonFinalPublicMethod() {}
  
}

class SubClass extends SuperClass {
  
  SuperClass() {
    super.finalMethod();
    super.nonFinalPublicMethod(); // Noncompliant
  }
  
}

final class FinalClass {
  
  FinalClass() {
    nonFinalPublicMethod();
  }
  
  public void nonFinalPublicMethod() {}
  
}

class OuterClass {
  
  public void nonFinalPublicMethod() {}
  
  class InnerClass {
    InnerClass() {
      nonFinalPublicMethod();
    }
  }
  
}
