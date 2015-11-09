class Accessibility {
  private class Example1 {
    class A1 {
    }
    class B1 {
      class A1 {
      }
    }

    class C1 extends B1 {
      class D1 extends A1 { // A1 = B1.A1, shadowing Example1.A1
      }
    }
  }

  private class Example2 {
    int j;

    class A2 {
    }

    void foo() {}

    class B2 {
      private int j;
      private class A2 {}
      private void foo() {}
    }

    class C2 extends B2 {
      class D2 extends A2 {} // A2 = Example2.A2, B2.A2 innacessible
      int i = j; // j = Example2.j, B2.j innacessible
      void bar() {
        foo(); // foo = Example2.foo, B2.foo innacessible
      }
    }
  }

}
