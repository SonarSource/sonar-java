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

class Foo17 {
  public Foo17() {
    // do something
  }
}

public class Main { // Compliant - contains main method
  public static void main(String[] args) throws Exception {
    System.out.println("Hello world!");
  }
}

public class NotMain { // Noncompliant
  static void main(String[] args) throws Exception {
    System.out.println("Hello world!");
  }

  static int main(String[] args) {
    System.out.println("Hello world!");
  }
}

public class MySingleton {
  private MySingleton() {
    // use getInstance()
  }

  private static class InitializationOnDemandHolderMySingleton { // compliant inner class is private, adding a private constructor won't change anything
    static final MySingleton INSTANCE = new MySingleton();
  }
  static class InitializationOnDemandHolderMySingleton2 { // Noncompliant
    static final MySingleton INSTANCE = new MySingleton();
  }
  private class InitializationOnDemandHolderMySingleton3 {
    static final MySingleton INSTANCE = new MySingleton();
  }
    public static MySingleton getInstance() {
    return InitializationOnDemandHolderMySingleton.INSTANCE;
  }
}
