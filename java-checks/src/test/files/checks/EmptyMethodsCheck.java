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

  // Noncompliant@+1
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

  // Noncompliant@+1
  private <T> void f() {
  }

  // Noncompliant@+1
  private int f() {
  }

  // Compliant
  private int f() {
    return 0;
  }

  // Compliant
  private abstract void f();
  
  // Noncompliant@+1
  private void f() {
    ;
  }

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
      // Noncompliant@+1
      private void g() {
      }
    }
  }

  Foo bar = new IFoo() {
    // Noncompliant@+1
    public void f() {
    }
  };
}

enum AEnum {
  ;

  // Noncompliant@+1
  public void f() {
  }

  public void f() {
    // Compliant
  }
}

class ANestedEnum {
  enum B {
    ;

    // Noncompliant@+1
    public void f() {
    }
  }
}

public interface IFoo {

  static IFoo FOO = new IFoo() {
    // Noncompliant@+1
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
    // Noncompliant@+1
    public void foo() {
    }

    // Compliant
    public int bar() {
      return 0;
    }
  };

  // Noncompliant@+1
  public void foo() {
  }

}
