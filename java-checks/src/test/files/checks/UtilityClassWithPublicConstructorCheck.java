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

class Foo9 { // Compliant

  public int foo;

  public static void foo() {
  }

}

class Foo10 { // Non-Compliant

  public static int foo;

  ;

}

class Foo11 {

  protected Foo11() { // Compliant
  }

  public static int a;

}

class Foo12 { //Non-compliant
  static class plop {
    int a;
  }
}

class Foo13 { // Compliant
  
  private Foo13() {
  }
  
  ;
}
