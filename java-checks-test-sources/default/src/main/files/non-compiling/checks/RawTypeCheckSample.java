package checks;

class RawTypeCheckSample {
  class KnownParent {
    public void method(Comparable arg1, String arg2) {} // Noncompliant
  }

  class ChildWithKnownHierarchy extends KnownParent {
    @Override
    public int compare(Comparable first, Comparable second) { // Compliant because of the "@Override" annotation
      checkNotNull(first);
      if (first == second) {
        return 0;
      }

      return second.compareTo(first);
    }

    public void checkNotNull(Comparable left) { // Noncompliant [[sc=30;ec=40]]
    }

    public void method(Comparable arg1, Unknown arg2) { // Compliant, unknown arg2 type, so may override
    }
  }

  class ChildWithUnknownHierarchy extends UnkownParent {
    public void checkNotNull(Comparable left) { // Compliant, !private and !static, so may override
    }
    private void privateCheckNotNull(Comparable left) { // Noncompliant
    }
    public static void staticCheckNotNull(Comparable left) { // Noncompliant
    }
  }
}
