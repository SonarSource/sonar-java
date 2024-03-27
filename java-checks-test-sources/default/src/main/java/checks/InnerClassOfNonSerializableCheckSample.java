package checks;

import java.io.Serializable;

class InnerClassOfNonSerializableCheckSample implements Serializable {
  
  class A1 implements Serializable {}

  void nonStaticMethod() {
    class X1 implements Serializable {}
  }
}

class InnerClassOfNonSerializableCheckSampleB {
  
  Serializable Anon = new Serializable() {}; // Noncompliant [[sc=27;ec=39]] {{Make this inner class static}}

  class B1 {}
  class B2 implements Cloneable {}
  class B3 implements Serializable {} // Noncompliant [[sc=9;ec=11]]{{Make this inner class static}}
  class B4 extends B3 {}
  static class B5 implements Serializable {}
  
  void nonStaticMethod() {
    class X1 implements Serializable {} // Noncompliant [[sc=11;ec=13]] {{Make "nonStaticMethod" static}}
    Object o = new X1(){};
  }
  
  static void staticMethod() {
    class X1 implements Serializable {}
  }
  
}
