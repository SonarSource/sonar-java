package checks;

class RawTypeCheck {
  class Child extends UnkownParent {
    @Override
    public int compare(Comparable first, Comparable second) { // Compliant because overriding
      checkNotNull(first);
      if (first == second) {
        return 0;
      }

      return second.compareTo(first);
    }

    private void checkNotNull(Comparable left) { // Noncompliant [[sc=31;ec=41]]
    }
  }
}
