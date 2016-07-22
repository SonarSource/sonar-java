import java.io.Serializable;
import java.lang.Object;

class Foo1 {
}

class Foo2 {
  public int foo() {
  }
}

class Foo3 { // Noncompliant [[sc=7;ec=11]] {{Add a private constructor to hide the implicit public one.}}
  public static void foo() {
  }
}

class Foo4 {
  public static int foo() {
  }

  public void bar() {
  }
}

class Foo5 {
  public Foo5() { // Noncompliant {{Hide this public constructor.}}
  }

  public static int foo() {
  }
}

class Foo6 {
  private Foo6() {
  }

  public static int foo() {
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

class Foo14 { // Noncompliant [[sc=7;ec=12]] {{Add a private constructor to hide the implicit public one.}}
  static {
  }
}

class Foo15 {
  public Object o = new Object() {
    public static void foo() {}
  };
}

class Foo16 implements Serializable { // Compliant
  private static final long serialVersionUID = 1L;
}
