package checks;

class A {
  private final int object = (Type<?>) foo; // Noncompliant
  private final int[] foo = new int[42];    // Compliant
}

static class C { // Does not compile, can not be static
  private final java.util.function.Consumer<Object> o = this::someMethod; // compliant
  private final java.util.function.Consumer<Object> o1 = new C()::someMethod; // compliant
  C c = new C();
  private final java.util.function.Consumer<Object> o2 = c::someMethod; // compliant
  private final java.util.function.Consumer<Object> o3 = C::someMethod2; // Noncompliant

  void someMethod(Object o) {
    return;
  }
  static void someMethod2(Object o) {
    return;
  }
}

class ConstantsShouldBeStaticFinalCheckDemo {
  interface Something {
  }
  Something getSomething() {
    return new Something() {
      private final long creation2; // Not initialized, does not compile
    };
  }
}
