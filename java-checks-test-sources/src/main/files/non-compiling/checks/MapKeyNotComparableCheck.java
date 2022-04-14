package checks;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class MapKeyNotComparableCheck {
  abstract class B<T> {
    Map<T, Object> nonComparable = new HashMap<>();
  }

  static class MyKeyType {
    @Override
    public String toString() {
      return super.toString();
    }
  }

  static class MyComparableType implements Comparable {
    @Override
    public int compareTo(@NotNull Object o) {
      return 0;
    }
  }

  static class Program {
    Map<UnknownType, Object> nonComparable = new HashMap<>(); // Compliant false positive
    Map<MyKeyType, Object> nonCompliant = new HashMap<>(); // Noncompliant [[sc=9;ec=18]] {{The key type should implement Comparable.}}
    Map<MyComparableType, Object> comparable = new HashMap<>(); // compliant
    Map noParams = new HashMap();

    Map<MyKeyType, Object> buildNonComparableMap() { // Noncompliant [[sc=9;ec=18]] {{The key type should implement Comparable.}}
      return nonComparable;
    }

    Map<MyComparableType, Object> buildComaparableMap() {
      return comparable;
    }
  }

}
