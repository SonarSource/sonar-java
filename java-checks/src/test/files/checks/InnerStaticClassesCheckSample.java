package foo;

class Fruit {
  private int value;

  public class Seed { // Noncompliant {{Make this a "static" inner class.}}
//             ^^^^
    int germinationDays = 0;
    public Seed(int germinationDays) {
      this.germinationDays = germinationDays;
    }
    public int getGerminationDays() {
      return germinationDays;
    }
    public class SeedI {  // Compliant, inner classes cannot be static
      int germinationDays = 0;
      public SeedI(int germinationDays) {
        this.germinationDays = germinationDays;
      }
      public int getGerminationDays() {
        return germinationDays;
      }
    }
  }

  public static class StaticSeed    {
    int germinationDays = 0;
    public StaticSeed(int germinationDays) {
      this.germinationDays = germinationDays;
    }
    public int getGerminationDays() {
      return germinationDays;
    }
    public static class StaticSeedI {
      int germinationDays = 0;
      public StaticSeedI(int germinationDays) {
        this.germinationDays = germinationDays;
      }
      public int getGerminationDays() {
        return germinationDays;
      }
    }
    public class StaticSeedI2 { // Noncompliant {{Make this a "static" inner class.}}

    }
  }

  public class Referencing {
    public Referencing() {
      value = 5;
    }
    public class ReferencingI {
      public ReferencingI() {
        value = 5;
      }
    }
  }

  public class Referencing2 {
    public Referencing2() {
      Fruit.this.value = 6;
    }
    public class Referencing2I {
      public Referencing2I() {
        Fruit.this.value = 6;
      }
    }
  }

  public class Referencing3 {
    public void method() {
      instance.toString(); // coverage
      new Referencing2(); // Referencing2 accesses instance
    }
  }

  private void fun() {
    foo:
    for (int i = 0; i < value; i++) {
      continue foo;
    }
    Fruit fruit = new Fruit() { // compliant, we ignore anonymous classes
    };
  }

  private static final Fruit.StaticSeed instance = new Fruit.StaticSeed(5){
  };

  public interface Interface {
  }

  public void noncompliant() {
    class Seed implements Interface { // Noncompliant {{Extract this local class into a "static" inner class.}}
      public void foo() {
        return;
      }
    }

    Interface inter = new Interface() { // compliant, we ignore anonymous classes
      public void foo() {
        return;
      }
    };
  }

}
class Bar {
  public int plop() {return 1;}
}

class A {
  private int fielda;

  class B { // Compliant inner class refers to field.
    Bar foo() {
      return new String() {
        public String toString() { return ""+fielda;}
      };
    }
  }
  class C { // Noncompliant
    Object foo() {
      return new Object();
    }
  }
  
  class SubB extends B {  // Compliant since super class is not static
    Object foo() {
      return new Object();
    }
  }
  
  class SubSubB extends SubB {  // Compliant since super class is not static
    Object foo() {
      return new Object();
    }
  }
  
  class InnerObject extends Object { // Noncompliant
    
  }
  
  class InnerArrayList extends java.util.ArrayList<String> { // Noncompliant
    
  }

}

public class Extendable {
  protected int field;
}

public class Foo extends Extendable {

  class Inner { // Compliant field is refered.
    void plop(){
      System.out.println(field);
    }
  }
}
public class A1 {
  private int field;

  class B { // Noncompliant
    int foo;
    class C {
      Bar foo() {
        return new Bar() {
          public String toString() {
            return "" + foo;
          }
        };
      }
    }
  }

  void usedInAnonymousClass() {
    final String myString = "";
    method(new toImplement() { // Compliant my String is used.
      void fun(String s) {
        myString.length();
      }
    });
  }
  interface toImplement {
    void fun(String s);
  }
  void method(Object j) {}

}

class A2 extends A3 {
  A2(Object o){}
}

class A3 {
  class Inner extends A2 {
    public Inner() {
      super(A3.this);
    }
  }
}
public class A4 {
  public void m(int a) {
    class B { // compliant cannot be made static
      void foo() {
        return a;
      }
    }
  }
}

class Parent {
  static class Generic<T> {
    abstract class A implements java.util.List<T> { } // Compliant
    abstract class B extends A { } // Compliant
    class C extends Generic<T> { } // Compliant
    class C2 extends Other.Generic<T> { } // Compliant
    abstract class D<T> implements java.util.List<T> { } // Noncompliant
  }
}
class Other {
  static class Generic<X> {}
}

class Foo2 {
  private String string;

  public Foo2() {
    class Bar { // Noncompliant {{Extract this local class into a "static" inner class.}}
    }
  }

  public void method() {
    class Baz { // Noncompliant {{Extract this local class into a "static" inner class.}}
    }
  }

  record InnerFoo2Record() { // Compliant
  }

  enum InnerEnum { // Compliant
    A, B, C;
  }

  @interface InnerAnnotation { // Compliant
  }
}

record FooRecord() {
  public FooRecord() {
    class InnerBar { // Noncompliant {{Extract this local class into a "static" inner class.}}
    }
  }

  public void method() {
    class InnerBaz { // Noncompliant {{Extract this local class into a "static" inner class.}}
    }
  }

  class InnerFoo { // Noncompliant
  }

  record InnerFooRecord() { // Compliant
  }

  enum InnerEnum { // Compliant
    A, B, C;
  }

  @interface InnerAnnotation { // Compliant
  }
}

@interface FooAnnotation {
  class InnerFoo { // Compliant
  }

  record InnerFooRecord() { // Compliant
  }
}
