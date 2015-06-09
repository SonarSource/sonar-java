abstract class A {
  private int b;

  abstract void method();
}
abstract class B { // Noncompliant {{Convert the abstract class "B" into an interface}}
  int method(){
    return 1;
  }
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