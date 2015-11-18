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

class D {
  public D(String s) {
  }
  
  public class C extends D {
    C() {
      super(null); // Compliant
    }
  }
}

public class Parent {

  public Parent () {
    doSomething();  // Noncompliant
  }

  public void doSomething () {  // not final; can be overridden
  }
}

public class Child extends Parent {

  private String foo;

  public Child(String foo) {
    super(); // leads to call doSomething() in Parent constructor which triggers a NullPointerException as foo has not yet been initialized
    this.foo = foo;
  }

  public void doSomething () {
    System.out.println(this.foo.length());
  }

}
