import java.io.Serializable;

class A implements Serializable {
  
  class A1 implements Serializable {}

  void nonStaticMethod() {
    class X1 implements Serializable {}
  }
}

class B {
  
  class B1 {}
  class B2 implements Cloneable {}
  class B3 implements Serializable {} // Noncompliant {{Make this inner class static}}
  class B4 extends B3 {}
  static class B5 implements Serializable {}
  
  void nonStaticMethod() {
    class X1 implements Serializable {} // Noncompliant {{Make "nonStaticMethod" static}}
  }
  
  static void staticMethod() {
    class X1 implements Serializable {}
  }
  
}
