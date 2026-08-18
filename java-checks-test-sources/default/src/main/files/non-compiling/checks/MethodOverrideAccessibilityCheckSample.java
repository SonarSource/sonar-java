package checks;

class MethodOverrideAccessibilityCheckSample {

  // --- Compliant: unknown parameter types should not trigger hiding detection ---

  static class ParentWithUnknownParam {
    static void process(Unknown param) {}
  }

  static class ChildWithUnknownParam extends ParentWithUnknownParam {
    public static void process(Unknown param) {} // Compliant - parameter type is unknown
  }

  // --- Noncompliant: hiding still detected when class has partial semantics ---

  static class ParentWithKnownStatic {
    protected static void compute(int x) {}
  }

  static class ChildHidingWithUnknownField extends ParentWithKnownStatic {
    Unknown field;
    public static void compute(int x) {} // Noncompliant {{Increase of accessibility from "protected" to "public" when hiding method.}}
  }

  // --- Compliant: override from unknown parent type ---

  static class ChildOfUnknownParent extends UnknownParent {
    @Override
    public void doSomething() {} // Compliant - parent is unknown, no overridden symbols resolved
  }

  // --- Compliant: static method with same name as instance method in superclass (compile error in Java) ---

  static class ParentWithInstanceMethod {
    protected void process() {}
  }

  static class ChildStaticSameAsInstance extends ParentWithInstanceMethod {
    public static void process() {} // Compliant - parent method is not static
  }

  // --- Compliant: static method in class extending unknown parent ---

  static class StaticChildOfUnknownParent extends UnknownParent {
    public static void staticMethod() {} // Compliant - unknown superclass, no hiding detected
  }

}
