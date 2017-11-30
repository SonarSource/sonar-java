import java.lang.Object;

class A { // Noncompliant [[sc=7;ec=8;secondary=4]] {{Add a constructor to the class, or provide default values.}}
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

enum Enum { // Noncompliant [[secondary=27]] {{Add a constructor to the enum, or provide default values.}}
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

