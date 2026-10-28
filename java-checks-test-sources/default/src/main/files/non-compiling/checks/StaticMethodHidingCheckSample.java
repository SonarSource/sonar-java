package checks;

class StaticMethodHidingCheckSample {

  // --- Noncompliant: hiding is still detected with partial semantic ---

  static class ParentWithKnownMethod {
    static void doWork() {
    }
  }

  static class ChildHidingWithUnknownField extends ParentWithKnownMethod {
    Unknown field; // causes partial semantic, but hiding is still detectable
    static void doWork() { // Noncompliant {{Rename this method; it hides "doWork" in "ParentWithKnownMethod".}}
    }
  }

  // --- Compliant: unknown parameter types should not raise issues ---

  static class ParentWithUnknown {
    static void process(Unknown param) {
    }
  }

  static class ChildWithUnknown extends ParentWithUnknown {
    static void process(Unknown param) { // Compliant - parameter type is unknown
    }
  }

  // --- Noncompliant: unknown return types are not considered covariant ---

  static class ParentWithUnknownReturn {
    static Unknown create() {
      return null;
    }
  }

  static class ChildWithUnknownReturn extends ParentWithUnknownReturn {
    static Unknown create() { // Noncompliant {{Rename this method; it hides "create" in "ParentWithUnknownReturn".}}
      return null;
    }
  }

}
