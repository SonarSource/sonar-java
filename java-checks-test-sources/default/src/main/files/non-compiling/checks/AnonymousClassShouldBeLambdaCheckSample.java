import checks.ThisInstanceTest.InvokeStatic;

import static checks.ThisInstanceTest.InvokeStatic.staticFunc;

abstract class AbstractClass {
  public abstract void foo();

  static void bar() {
    AbstractClass ac1 = new AbstractClass() { // Compliant: not a SAM
      @Override
      public void foo() {
      }
    };

    Unknown u = new Unknown() { // Compliant: can not resolve parent
      @Override
      void foo() {
      }
    };
  }

  interface InvokeStatic {

    int func();

    static int staticFunc() {
      InvokeStatic f = new InvokeStatic() { // Noncompliant
        @Override
        public int func() {
          unknown();
          staticFunc();
          return 0;
        }
      };
      return f.func();
    }
  }
}
