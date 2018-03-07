class test{
  class A implements Comparable<A> {
    
    public int compareTo(A a) {
      return -1; // Compliant
    }
  }  
  class B extends A {
    
    public int compareTo(B b) {   // Noncompliant {{Refactor this method so that its argument is of type 'A'.}}
      return 0;
    }
    
    public int compareTo(A a) { // Compliant
      return 0;
    }
    
    public int compareTo(A a, B b) { // Compliant
      return 0;
    }
  }
  
  class C implements Comparable{
    public int compareTo(Object a) {  // Compliant
      return 0;
    }
  }
  
  class E extends C {
    public int compareTo(E e) {   // Noncompliant  {{Refactor this method so that its argument is of type 'Object'.}}
      return -1;
    }
  }
  
  static class Bar implements Comparable<A> {  // Compliant
    public int compareTo(A rhs) {
      return -1;
    }
  }
          
  static class FooBar extends Bar {
      public int compareTo(FooBar rhs) {  // Noncompliant: Parameter should be of type Bar
        return 0;
      }
  }
    
  class D {
    public int compareTo(A a) {      // Compliant
      return 1;
    }
    
  }
  
  class F extends B{
    public int compareTo1(B b) {      // Compliant
      return 1;
    }
  }
  
}
