package checks;

import org.immutables.value.Value;

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

@Value.Immutable
abstract class Bar { // Compliant
  abstract String name();
}

@org.immutables.value.Value.Immutable
abstract class BarWithFullAnnotation { // Compliant
  abstract String name();
}

@creedthoughts.org.immutables.value.Value.Immutable
abstract class BarWithFullAnnotation2 { // Compliant
  abstract String name();
}
