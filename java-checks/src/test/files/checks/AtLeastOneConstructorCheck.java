import org.springframework.beans.factory.annotation.Autowired;
import java.lang.Object;
import javax.inject.Inject;
class A { // Noncompliant [[sc=7;ec=8;secondary=5]] {{Add a constructor to the class, or provide default values.}}
  private int field;
}

class B {
  private int field;

  B() {
    field = 0;
  }
}

class C {
  public int field;
  private static int field2;
  void foo() {
    Object o = new Object() {
      private int field;
    };
  }
}

enum Enum { // Noncompliant [[secondary=28]] {{Add a constructor to the enum, or provide default values.}}
  A;
  private int field;
}

abstract class D {
  private int field;
}

class E {
  private int field = 5;
}

class EJB  {
  @javax.ejb.EJB
  private MyObject foo; // injection via EJB
}

@javax.ejb.EJB // injection via EJB
class EJB2 {
  private Object someObject;
}

class MyService {}
class Spring1 {
  @Autowired
  private MyService myService;
}
class Spring2 { // Noncompliant [[secondary=57]]
  @Autowired
  private MyService myService;
  private MyService myService2;
}

class Inject1 {
  @Inject
  private MyService myService;
}
class Inject2 { // Noncompliant [[secondary=67]]
  @Inject
  private MyService myService;
  private MyService myService2;
}
