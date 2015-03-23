public class Parent {
  public void foo() {  }
  void qix();
  public static void staticMethod(){};
}
public class Outer {
  public static void staticMethod(){};
  public void foo() {  }
  public void plop() {  }
  public class Inner extends Parent {
    public void doTheThing() {
      foo();  // Noncompliant; was Outer.this.foo() intended instead?
      super.foo(); //Compliant: unambiguous
      Outer.this.foo(); //Compliant: unambiguous
      bar();//Compliant : symbol is unresolved.
      qix(); //Compliant: No ambiguity, not defined in outer class
      doTheThing();//Compliant not from super type
      staticMethod(); //Compliant: static method.
    }
  }
  public interface I extends I2{
    default void toto() {
      plop();//NonCompliant
    }
  }
  public interface I2  {
    void plop();
  }
}

public class Child extends Parent {
  void fun() {
    foo();
  }
}

class OuterClass {
  void foo() {}

  class innerClass extends OuterClass {
    void fun() {
      foo();
    }
  }
}
class Foo extends OuterClass2 {}
class OuterClass2 {
  void foo() {}

  class innerClass extends Foo {
    void fun() {
      foo();
    }
  }
}
class OuterClass3 {
  void foo(){}
  class innerClass {
    void foo() {}
    void fun() {
      foo();
    }
  }
}

class GenericParent<T> {
  T foo(){}
}
class OuterClass4 {
  Object foo();
  class innerClass<T> extends GenericParent<T> {
    void bar() {
      foo(); // NonCompliant
    }
  }
}