package checks;

class EqualsNotOverriddenInSubclassCheck {

  static final String CONST = "constant";
  static String staticMember = "static member";
  String s1;

  class A {
    void foo() {
      Object o = new A() {
        int inner;
      };
    }

    public boolean equals(Object obj) {
      return true;
    }
  }

  class B extends A { // Noncompliant [[sc=9;ec=10]]  {{Override the "equals" method in this class.}}
    String s2;
  }

  abstract class C {
    boolean equals;

    public abstract boolean equals(Object obj);
  }

  class D extends C { // Compliant
    int i;

    @Override
    public boolean equals(Object obj) {
      return false;
    }
  }

  class E {
    public boolean equals() {
      return true;
    }
  }

  class F extends E { // Compliant
    String s;

    public boolean equals(int i) {
      return true;
    }
  }

  class G {
    public boolean equals(Object obj) {
      return true;
    }
  }

  class H extends G { // Compliant
    String s;

    @Override
    public boolean equals(Object obj) {
      return false;
    }
  }

  class J {
    String s;
  }

  class M {
    @Override
    public final boolean equals(Object obj) {
      return false;
    }
  }

  class N extends M { // Compliant - M.equals() is final
    int i;
  }

  class O extends A {
    @Override
    public final boolean equals(Object obj) {
      return false;
    }
  }

  class P extends O { // Compliant - O.equals() is final
    String s;
  }

  class Q extends A {
    private String name;
    @Override
    public final boolean equals(Object o) { return false; }
  }

}


