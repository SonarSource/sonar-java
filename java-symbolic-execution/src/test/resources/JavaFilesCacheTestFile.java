package org.sonar.java;

public class JavaFilesCacheTestFile {

  public void foo() {}

  public void bar() {}

  static class A {
    @SuppressWarnings("qix")
    int field;

    interface I {
      @SuppressWarnings("all")
      void foo();
    }

    private void method() {
      @SuppressWarnings("all")
      class B {
        Object obj = new I() {

          @SuppressWarnings({"foo", "bar"})
          @Override
          public void foo() {

          }
        };
      }
    }

    @java.lang.SuppressWarnings({"qix"})
    I implem = new I() {
      @Override
      public void foo() {
      }
    };

    private void foo(@SuppressWarnings("gul") int b) {

      @SuppressWarnings("gul")
      Object obj = new I() {
        @Override
        public void foo() {
        }
      };
    }
  }

  @java.lang.Deprecated
  @interface plop {
  }
}
