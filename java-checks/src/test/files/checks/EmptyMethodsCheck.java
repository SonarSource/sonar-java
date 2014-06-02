class A {
  // Compliant
  public A() {
  }

  // Compliant
  private A() {
  }

  // Compliant
  private <T> A() {
  }

  // Non-Compliant
  private void f() {
  }

  // Compliant
  private void f() {
    /* hmm */
  }

  // Compliant
  private void f() {
    throw new UnsupportedOperationException();
  }

  // Non-Compliant
  private <T> void f() {
  }

  // Non-Compliant
  private int f() {
  }

  // Compliant
  private int f() {
    return 0;
  }

  // Compliant
  private abstract void f();

}

abstract class AAbstract {
  // Compliant
  private void f() {
    abstract class B {
      // Compliant
      private void g() {
      }
    }

    static class C {
      // Non-Compliant
      private void g() {
      }
    }
  }

  Foo bar = new IFoo() {
    // Noncompliant
    public void f() {
    }
  };
}

enum AEnum {
  ;

  // Non-Compliant
  public void f() {
  }

  public void f() {
    // Compliant
  }
}

class ANestedEnum {
  enum B {
    ;

    // Non-Compliant
    public void f() {
    }
  }
}

public interface IFoo {

  static IFoo FOO = new IFoo() {
    // Noncompliant
    public void foo() {
    }

    // Compliant
    public void bar() {
      System.out.println();
    }
  };

}

enum Foo {

  FOO {
    // Noncompliant
    public void foo() {
    }

    // Compliant
    public int bar() {
      return 0;
    }
  };

  // Noncompliant
  public void foo() {
  }

}
