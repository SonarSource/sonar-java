package checks;

class EqualsNotOverriddenInSubclassCheckSample {

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

  class B extends A { // Noncompliant {{Override the "equals" method in this class.}}
//      ^
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

  abstract class AbstractEquals {
    @Override
    abstract public boolean equals(Object obj);
  }

  abstract class BAbstractEquals extends AbstractEquals { // Compliant, parent is not overriding the content of equals.
    String field = "";
  }

  class CAbstractEquals extends BAbstractEquals { // Compliant, you have to override equals anyway
    private String field = "";

    @Override
    public boolean equals(Object obj) {
      return false;
    }
  }

  abstract class AbstractClass {
    @Override
    public boolean equals(Object obj) {
      return true;
    }
  }

  abstract class BAbstractClass extends AbstractClass { // Noncompliant
    private String field = "";
  }

  class CAbstractClass extends BAbstractClass { // Noncompliant
    private String field2 = "";
  }

  abstract class BAbstractClass2 extends AbstractClass {
    @Override
    public boolean equals(Object obj) {
      return true;
    }
  }

  abstract class CAbstractClass2 extends BAbstractClass2 { // Noncompliant
    private String field2 = "";
  }

}


