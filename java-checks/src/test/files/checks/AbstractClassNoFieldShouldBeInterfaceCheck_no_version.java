abstract class A {
  private int b;

  abstract void method();
}
abstract class B { // Noncompliant {{Convert the abstract class "B" into an interface. (sonar.java.source not set. Assuming 8 or greater.)}}
  int method(){
    return 1;
  }
  class F {}
}
class C {
  int method(){
    return 1;
  }
}

abstract class D {
  protected void method() {

  }
}

abstract class E extends A {
}
