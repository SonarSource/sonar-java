class Foo1 { // Compliant
}

class Foo2 { // Compliant
  public int foo() {
  }
}

class Foo3 { // Non-Compliant
  public static void foo() {
  }
}

class Foo4 { // Compliant
  public static int foo() {
  }

  public void bar() {
  }
}

class Foo5 {
  public Foo5() { // Non-Compliant
  }

  public static int foo() {
  }
}

class Foo6 { // Compliant
  private Foo6() {
  }

  public static int foo() {
  }

  int foo;

  static int bar;
}

class Foo7 {

  public <T> Foo7(T foo) { // Non-Compliant
  }

  public static <T> void foo(T foo) {
  }

}

class Foo8 extends Bar { // Compliant

  public static void f() {
  }

}
