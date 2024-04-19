package checks;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class MapKeyNotComparableCheckSample {
  static class NonComparable {
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

  static class UnknownClass implements Unknown {}

  static class Program {
    Map<UnknownClass, Object> nonComparable = new HashMap<>(); // Potential false negative caused by unknown hierarchy
    Map<UnknownType, Object> nonComparable = new HashMap<>(); // Potential false negative caused by unknown symbol
    Map<NonComparable, Object> nonCompliant = new HashMap<>(); // Noncompliant [[sc=9;ec=22]] {{The key type should implement Comparable.}}
    Map<MyComparableType, Object> comparable = new HashMap<>(); // compliant
    Map noParams = new HashMap();

    Map<NonComparable, Object> buildNonComparableMap() { // Noncompliant [[sc=9;ec=22]] {{The key type should implement Comparable.}}
      return nonComparable;
    }

    Map<MyComparableType, Object> buildComaparableMap() {
      return comparable;
    }
  }

}
