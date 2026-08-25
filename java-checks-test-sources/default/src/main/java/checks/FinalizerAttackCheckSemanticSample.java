package checks;

class FinalizerAttackCheckSemanticSample {

  // --- Noncompliant: field initializer calls a method that declares checked exception ---

  static class FieldInitializerCheckedThrow { // Noncompliant {{Make this class "final" or add a private constructor, because initializers can throw.}}
    private final Object value = loadValue(); // Secondary {{Throwing initializer}}

    private static Object loadValue() throws Exception {
      return new Object();
    }
  }

  // --- Noncompliant: field initializer constructs an object whose constructor declares checked exception ---

  static final class ThrowingConstructorTarget {
    public ThrowingConstructorTarget(String s) throws Exception {
      if (s == null) throw new Exception();
    }
  }

  static class FieldInitializerNewThrows { // Noncompliant {{Make this class "final" or add a private constructor, because initializers can throw.}}
    private final ThrowingConstructorTarget svc = new ThrowingConstructorTarget("token"); // Secondary {{Throwing initializer}}
  }

  // --- Noncompliant: field initializer with explicit constructor, method declares checked exception ---

  static class FieldInitWithCheckedAndCtor { // Secondary {{Non-final class}}
    private final Object data = initField();

    public FieldInitWithCheckedAndCtor() { // Noncompliant
    }

    private static Object initField() throws Exception {
      return new Object();
    }
  }

  // --- Compliant: field initializer calls method with no declared exceptions ---

  static class FieldInitNoDeclaredThrows {
    private final Object value = compute();

    private static Object compute() {
      throw new UnsupportedOperationException();
    }
  }
}
