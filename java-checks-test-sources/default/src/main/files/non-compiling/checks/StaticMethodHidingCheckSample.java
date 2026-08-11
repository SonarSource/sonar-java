package checks;

class StaticMethodHidingCheckSample {

  // --- Compliant: unknown parameter types should not raise issues ---

  static class ParentWithUnknown {
    static void process(Unknown param) {
    }
  }

  static class ChildWithUnknown extends ParentWithUnknown {
    static void process(Unknown param) { // Compliant - parameter type is unknown
    }
  }

}
