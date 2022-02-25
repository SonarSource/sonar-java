package checks;

class CallSuperMethodFromInnerClassCheck {

  class Parent {
    public void foo() {}
    void qix() {}
    public static void staticMethod() {}
  }

  class Outer {
    public static void staticMethod() {}
    public void foo() {}
    public void plop() {}
    class Inner extends Parent {
      public void doTheThing() {
        foo();  // Noncompliant {{Prefix this call to "foo" with "super.".}}
        super.foo(); //Compliant: unambiguous
        Outer.this.foo(); //Compliant: unambiguous
        qix(); //Compliant: No ambiguity, not defined in outer class
        doTheThing();//Compliant not from super type
        staticMethod(); //Compliant: static method.
      }
    }
    public interface I extends I2 {
      default void toto() {
        plop();// Noncompliant [[sc=9;ec=13]] {{Prefix this call to "plop" with "super.".}}
      }
    }
    public interface I2 {
      void plop();
    }
  }

  class Child extends Parent {
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
    void foo() {}
    class innerClass {
      void foo() {}
      void fun() {
        foo();
      }
    }
  }

  class GenericParent<T> {
    T foo() { return null; }
  }
  class OuterClass4 {
    Object foo() { return null; }
    class innerClass<T> extends GenericParent<T> {
      void bar() {
        foo(); // Noncompliant {{Prefix this call to "foo" with "super.".}}
      }
    }
  }

  abstract class BaseEncoding {
    BaseEncoding() {}

    public static final class DecodingException extends java.io.IOException {
      DecodingException(String message) {
        super(message); // Compliant
      }

      DecodingException(Throwable cause) {
        super(cause); // Compliant
      }
    }
  }

}
