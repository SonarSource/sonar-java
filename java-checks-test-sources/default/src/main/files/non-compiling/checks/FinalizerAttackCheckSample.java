package checks;

class FinalizerAttackCheckSample {

  // --- Noncompliant: sealed class permitting an unknown type (unresolvable) ---
  // When the permitted type cannot be resolved, the class is conservatively treated as safely sealed.
  // However, this sealed class also permits a non-sealed type that IS resolvable.

  static sealed class SealedWithUnknown permits UnknownType, KnownNonSealed { // Secondary {{Non-final class}}
    public SealedWithUnknown(String s) throws Exception { // Noncompliant
      if (s == null) throw new Exception();
    }
  }

  static non-sealed class KnownNonSealed extends SealedWithUnknown { // Secondary {{Non-final class}}
    KnownNonSealed(String s) throws Exception { // Noncompliant
      super(s);
    }
  }

  // --- Noncompliant: sealed class permitting only unknown types (conservatively unsafe) ---

  static sealed class SealedWithOnlyUnknown permits AnotherUnknownType { // Secondary {{Non-final class}}
    public SealedWithOnlyUnknown(String s) throws Exception { // Noncompliant
      if (s == null) throw new Exception();
    }
  }

  // --- Noncompliant: non-final class with throwing constructor (basic case) ---

  static class BasicThrowing { // Secondary {{Non-final class}}
    public BasicThrowing() throws Exception { // Noncompliant
      throw new Exception();
    }
  }
}
