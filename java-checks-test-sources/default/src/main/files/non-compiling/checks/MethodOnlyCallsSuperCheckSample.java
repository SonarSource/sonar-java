package checks;

class MethodOnlyCallsSuperCheckSample {
  abstract class A extends Unknown {
    // When the parent method is Unknown, we should not report anything
    private int f4() { // Compliant
      return super.f4();
    }

    @Override
    @SomeCrazyAnnotation
    private void f17() { // Not compiling and no issue
      super.f();
    }

    @SomeCrazyAnnotation
    @Override
    private void f18() { // Not compiling and no issue
      super.f();
    }

    @foo.Deprecated
    private <T> void f20() { // Not compiling and no issue
      super.f();
    }

    @Override
    @Override
    private <T> void f21() { // Compliant
      super.f21();
    }

    protected void bar2() {}
  }

  class C extends A {
    @Override
    void bar2() { // Compliant (but does not compile... can not reduce visibility [protected -> package])
      super.bar2();
    }

    @Override
    void f1() {
    }
  }
}
