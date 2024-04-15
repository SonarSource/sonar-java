package checks;

import java.io.Serializable;

class UtilityClassWithPublicConstructorCheckSample {

  class Foo1 {
  }

  class Foo2 {
    public void foo() {
    }
  }

  class Foo3 { // Noncompliant [[sc=9;ec=13]] {{Add a private constructor to hide the implicit public one.}}
    public static void foo() {
    }
  }

  class Foo4 {
    public static void foo() {
    }

    public void bar() {
    }
  }

  class Foo5 {
    public Foo5() { // Noncompliant {{Hide this public constructor.}}
    }

    public static void foo() {
    }
  }

  class Foo6 {
    private Foo6() {
    }

    public static void foo() {
    }

    int foo;

    static int bar;
  }

  class Foo7 {

    public <T> Foo7(T foo) { // Noncompliant
    }

    public static <T> void foo(T foo) {
    }

  }

  class Foo8 extends Bar {

    public static void f() {
    }

  }

  class Foo9 {

    public int foo;

    public static void foo() {
    }

  }

  class Foo10 { // Noncompliant

    public static int foo;

    ;

  }

  class Foo11 {

    protected Foo11() {
    }

    public static int a;

  }

  class Foo12 { // Noncompliant
    static class plop {
      int a;
    }
  }

  class Foo13 {

    private Foo13() {
    }

    ;
  }

  class Foo14 { // Noncompliant [[sc=9;ec=14]] {{Add a private constructor to hide the implicit public one.}}
    static {
    }
  }

  class Foo15 {
    public Object o = new Object() {
      public static void foo() {
      }
    };
  }

  class Foo16 implements Serializable { // Compliant
    private static final long serialVersionUID = 1L;
  }

  class Foo17 {
    public Foo17() {
      // do something
    }
  }

  class Main { // Compliant - contains main method
    public static void main(String[] args) throws Exception {
      System.out.println("Hello world!");
    }
  }

  class NotMain { // Noncompliant
    static void main(String[] args) throws Exception {
      System.out.println("Hello world!");
    }

    static void main2(String[] args) {
      System.out.println("Hello world!");
    }
  }

  public class MySingleton {
    private void MySingleton2() {
      // use getInstance()
    }

    private static class InitializationOnDemandHolderMySingleton { // compliant inner class is private, adding a private constructor won't change anything
      static final checks.MySingleton INSTANCE = new checks.MySingleton();
    }
    static class InitializationOnDemandHolderMySingleton2 { // Noncompliant
      static final checks.MySingleton INSTANCE = new checks.MySingleton();
    }
    private class InitializationOnDemandHolderMySingleton3 {
      static final checks.MySingleton INSTANCE = new checks.MySingleton();
    }

    public static checks.MySingleton getInstance() {
      return InitializationOnDemandHolderMySingleton.INSTANCE;
    }
  }

}
