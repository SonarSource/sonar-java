package checks;

import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

class EmptyMethodsCheckNoSemantics {
  class A {
    // Compliant - there is other constructors
    public A() {
    }

    public A(int c) {
    }

    // Noncompliant@+1 [[sc=18;ec=19]] {{Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or complete the implementation.}}
    private void f() {
    }

    // Compliant
    private void g() {
      /* hmm */
    }

    // Compliant
    private void h() {
      throw new UnsupportedOperationException();
    }

    // Compliant
    private int i() {
      return 0;
    }

    // Noncompliant@+1
    private void j() {
      ;
    }
  }

  class AwithGenerics {
    // Compliant
    private <T> AwithGenerics() {
    }

    // Noncompliant@+1
    private <T> void f() {
    }
  }

  abstract class Abstract {
    // Compliant
    private void f() {
      abstract class B {
        // Compliant
        private void g() {
        }
      }

      class C {
        // Noncompliant@+1
        private void g() {
        }
      }
    }

    IFoo bar = new IFoo() {
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

    public void g() {
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

  class Constructors {
    class C {
      // Noncompliant@+1
      public C() {
      }
    }

    class D {
      // Compliant
      public D() {
        // usefull comment
      }
    }

    class E {
      // Compliant - not public
      E() {
      }
    }

    class F {
      // Compliant - not a no-arg constructor
      public F(int i) {
      }
    }
  }

  record MyRecord() {
    // Noncompliant@+1
    void foo() {
    }
  }

  class QuickFixes {
    // Noncompliant@+1 [[sc=12;ec=22;quickfixes=qf0]]
    public QuickFixes() {}
    // fix@qf0 {{Insert placeholder comment}}
    // edit@qf0 [[sc=26;ec=26]] {{ /* TODO document why this constructor is empty */ }}

    // Noncompliant@+1 [[sc=18;ec=29;quickfixes=qf1]]
    private void emptyMethod() {
    }
    // fix@qf1 {{Insert placeholder comment}}
    // edit@qf1 [[sl=+0;el=+1;sc=33;ec=5]] {{\n      // TODO document why this method is empty\n    }}

    // Noncompliant@+1 [[sc=18;ec=30;quickfixes=qf2]]
    private void emptyMethod2() {

    }
    // fix@qf2 {{Insert placeholder comment}}
    // edit@qf2 [[sl=+0;el=+2;sc=34;ec=5]] {{\n      // TODO document why this method is empty\n    }}
  }

  private class ExceptionalCompliantCases {
    @org.aspectj.lang.annotation.Pointcut()
    void foo() {

    }

    @Pointcut()
    void bar() {}

    @Before("")
    void stillTriggerOnOtherAnnotations() {} // Noncompliant
  }
}
