package foo;

class Fruit {
  private int value;

  public class Seed {  // Noncompliant {{Make this a "static" inner class.}}
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
    Fruit fruit = new Fruit() { // Noncompliant {{Make this a named "static" inner class.}}
    };
  }

  private static final Fruit.StaticSeed instance = new Fruit.StaticSeed(5){
  };

  public interface Interface {
  }

  public void noncompliant() {
    class Seed implements Interface { // Noncompliant
      public void foo() {
        return;
      }
    }

    Interface inter = new Interface() { // Noncompliant
      public void foo() {
        return;
      }
    };
  }

}
public class Bar {
  public int plop() {return 1;}
}


public class A {
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
        return new String() {
          public String toString() {
            return "" + foo;
          }
        };
      }
    }
  }
}