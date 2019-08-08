class A implements Comparable<A> {

  int foo() {
    return 1; // Noncompliant [[sc=12;ec=13]] {{Remove this method and declare a constant for this value.}}
  }
  String bar() {
    return ""; // Noncompliant [[sc=12;ec=14]] {{Remove this method and declare a constant for this value.}}
  }
  char qix() {
    return 'c'; // Noncompliant [[sc=12;ec=15]] {{Remove this method and declare a constant for this value.}}
  }
  Object lum() {
    return new Object(); // Compliant
  }
  int gul() {
    System.out.println("foo");
    return 1;
  }
  abstract void bom();
  void bah(){
    return;
  }
  void tol(){
    System.out.println("");
  }

  @Override
  public String toString() {
    return "";  // compliant, this method is an override
  }
  // removed @Override annotation
  public int compareTo(A o) {
    return 0; // Compliant - method is an override
  }
  long gro() {
    return 1L; // Noncompliant [[sc=12;ec=14]] {{Remove this method and declare a constant for this value.}}
  }

  @MyAnnotation
  long puf() {
    return 1L; // Compliant
  }
}

@interface MyAnnotation {}

interface B {
  default String defaultMethodReturningConstant() {
    return "";
  }
}
